/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlin.concurrent.thread

/**
 * Manager class that orchestrates translation of proposition content using ML Kit Translation API.
 *
 * This class provides high-level translation operations for propositions and proposition items,
 * delegating the actual translation work to [PropositionTranslationUtility].
 */
internal class PropositionTranslationManager {

    companion object {
        private const val SELF_TAG = "PropositionTranslationManager"
    }
    
    private var translationUtility: PropositionTranslationUtility? = null
    private var isTranslationEnabled = false
    private var statusCallback: TranslationStatusCallback? = null
    
    /**
     * Sets the callback to receive translation status updates.
     *
     * @param callback the callback to receive status updates, or null to remove the callback
     */
    fun setTranslationStatusCallback(callback: TranslationStatusCallback?) {
        this.statusCallback = callback
    }
    
    /**
     * Enables automatic translation of proposition content from English to the device's native
     * language using ML Kit Translation API.
     *
     * This method should be called early in the application lifecycle to allow time for the
     * translation model to download. Translation will only occur if:
     *
     * - The device locale is not English
     * - The device language is supported by ML Kit Translation
     * - The translation model downloads successfully
     *
     * Note: Language models are approximately 30MB and will only be downloaded over Wi-Fi.
     * Models are cached on device for faster subsequent initializations.
     * 
     * Initialization happens asynchronously on a background thread to avoid blocking
     * and to allow Google Play Services time to initialize.
     * 
     * Status updates are provided via [TranslationStatusCallback] if set using
     * [setTranslationStatusCallback].
     */
    @VisibleForTesting
    fun enableTranslation() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, 
            "Enabling translation - starting background initialization")
        
        // Notify that initialization has started
        Handler(Looper.getMainLooper()).post {
            statusCallback?.onTranslationInitializationStarted()
        }
        
        // Run initialization on a background thread to avoid blocking
        // and give Google Play Services time to initialize
        thread(start = true, name = "PropositionTranslation-Init") {
            try {
                Thread.sleep(500) // Small delay to ensure Google Play Services is ready
                
                if (translationUtility == null) {
                    translationUtility = PropositionTranslationUtility()
                }
                
                // Set up a callback for the utility to notify us of download progress
                val progressCallback = object : PropositionTranslationUtility.DownloadProgressCallback {
                    override fun onCheckingCache() {
                        Handler(Looper.getMainLooper()).post {
                            statusCallback?.onCheckingModelCache()
                        }
                    }
                    
                    override fun onDownloadStarted(languageCode: String) {
                        Handler(Looper.getMainLooper()).post {
                            statusCallback?.onModelDownloadStarted(languageCode)
                        }
                    }
                }
                
                val result = translationUtility?.initialize(progressCallback)
                
                if (result != null) {
                    when (result.status) {
                        PropositionTranslationUtility.InitializationStatus.SUCCESS_MODEL_CACHED -> {
                            isTranslationEnabled = true
                            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG,
                                "Proposition translation enabled for language: ${result.languageCode} (model found in cache)"
                            )
                            // Notify callback on main thread
                            Handler(Looper.getMainLooper()).post {
                                result.languageCode?.let { lang ->
                                    statusCallback?.onModelFoundInCache(lang)
                                }
                            }
                        }
                        PropositionTranslationUtility.InitializationStatus.SUCCESS_MODEL_DOWNLOADED -> {
                            isTranslationEnabled = true
                            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG,
                                "Proposition translation enabled for language: ${result.languageCode} (model downloaded successfully)"
                            )
                            // Notify callback on main thread
                            Handler(Looper.getMainLooper()).post {
                                result.languageCode?.let { lang ->
                                    statusCallback?.onModelDownloadedSuccessfully(lang)
                                }
                            }
                        }
                        else -> {
                            isTranslationEnabled = false
                            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG,
                                "Proposition translation initialization completed but translation is not enabled: ${result.message}"
                            )
                            // Notify callback on main thread
                            Handler(Looper.getMainLooper()).post {
                                result.message?.let { msg ->
                                    statusCallback?.onTranslationInitializationFailed(msg)
                                }
                            }
                        }
                    }
                } else {
                    isTranslationEnabled = false
                    Log.warning(MessagingConstants.LOG_TAG, SELF_TAG,
                        "Proposition translation initialization returned null result"
                    )
                }
            } catch (e: Exception) {
                Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, 
                    "Failed to enable translation: ${e.message}")
                isTranslationEnabled = false
                // Notify callback on main thread
                Handler(Looper.getMainLooper()).post {
                    statusCallback?.onTranslationInitializationFailed("Exception: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Disables automatic translation of proposition content and releases translation resources.
     */
    @VisibleForTesting
    fun disableTranslation() {
        translationUtility?.cleanup()
        translationUtility = null
        isTranslationEnabled = false
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, 
            "Proposition translation has been disabled.")
    }
    
    /**
     * Translates the content of a proposition item from English to the device's native language.
     *
     * @param propositionItem the [PropositionItem] to translate
     * @return a new [PropositionItem] with translated content, or the original if
     *     translation is disabled or fails, or null if input is null
     */
    @VisibleForTesting
    fun translatePropositionItem(propositionItem: PropositionItem?): PropositionItem? =
        if (propositionItem == null || !isTranslationEnabled) propositionItem
        else translationUtility?.translatePropositionItem(propositionItem) ?: propositionItem
    
    /**
     * Translates all proposition items in a list of propositions.
     *
     * @param propositions the list of [Proposition]s to translate
     * @return a new list with translated propositions
     */
    fun translatePropositions(propositions: List<Proposition>): List<Proposition> {
        if (!isTranslationEnabled || translationUtility == null) {
            return propositions
        }
        
        return propositions.map { proposition ->
            try {
                val translatedItems = proposition.items.map { item ->
                    translationUtility?.translatePropositionItem(item) ?: item
                }
                
                // Create a new proposition with translated items
                Proposition(
                    proposition.uniqueId,
                    proposition.scope,
                    proposition.scopeDetails,
                    translatedItems
                )
            } catch (e: MessageRequiredFieldMissingException) {
                Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to create translated proposition: ${e.localizedMessage}"
                )
                // If translation fails, use the original proposition
                proposition
            }
        }
    }
    
    /**
     * Checks if proposition translation is currently enabled.
     *
     * @return `true` if translation is enabled, `false` otherwise
     */
    @VisibleForTesting
    fun isTranslationEnabled(): Boolean = isTranslationEnabled
    
    /**
     * Gets the target language code being used for translation.
     *
     * @return the target language code, or `null` if translation is not enabled
     */
    @VisibleForTesting
    fun getTargetLanguageCode(): String? = translationUtility?.getTargetLanguageCode()
}

