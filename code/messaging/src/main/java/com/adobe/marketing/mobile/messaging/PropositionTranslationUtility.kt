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

import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Locale

/**
 * Utility class for translating English text content in PropositionItems to the device's native
 * language using Google ML Kit Translation API.
 */
internal class PropositionTranslationUtility {
    
    companion object {
        private const val SELF_TAG = "PropositionTranslationUtility"
        private const val TRANSLATION_TIMEOUT_MS = 30_000L
        private const val MODEL_DOWNLOAD_TIMEOUT_MS = 60_000L
        
        // HTML tag pattern to preserve HTML structure during translation
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
    }
    
    /**
     * Enum representing the initialization status of the translation utility.
     */
    enum class InitializationStatus {
        SUCCESS_MODEL_CACHED,
        SUCCESS_MODEL_DOWNLOADED,
        FAILED_NO_LOCALE,
        FAILED_ENGLISH_LOCALE,
        FAILED_UNSUPPORTED_LANGUAGE,
        FAILED_MODEL_DOWNLOAD,
        FAILED_EXCEPTION
    }
    
    /**
     * Data class to hold the result of initialization.
     */
    data class InitializationResult(
        val status: InitializationStatus,
        val languageCode: String? = null,
        val message: String? = null
    )
    
    /**
     * Callback interface for reporting download progress.
     */
    interface DownloadProgressCallback {
        /**
         * Called when checking if the model is in cache.
         */
        fun onCheckingCache()
        
        /**
         * Called when model download starts.
         * @param languageCode the target language code
         */
        fun onDownloadStarted(languageCode: String)
    }
    
    private var translator: Translator? = null
    private var isTranslationEnabled = false
    private var targetLanguageCode: String? = null
    private val cacheManager = TranslationModelCacheManager()
    
    /**
     * Initializes the translation utility by detecting the device locale and downloading the
     * necessary translation model from ML Kit. Uses toLanguageTag() to get BCP 47 language tag
     * (e.g., "es-ES") and extracts the language code (e.g., "es") for more reliable detection.
     *
     * @param progressCallback optional callback to receive progress updates during initialization
     * @return [InitializationResult] containing the status and details of the initialization
     */
    fun initialize(progressCallback: DownloadProgressCallback? = null): InitializationResult {
        // First check the app's current locale (set by LocaleHelper or app configuration)
        // This takes precedence over the system device locale
        val appLocale = Locale.getDefault()
        val systemLocale = ServiceProvider.getInstance().deviceInfoService.activeLocale
        
        // Use app locale if available, otherwise fall back to system locale
        val deviceLocale = appLocale ?: systemLocale
        
        if (deviceLocale == null) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Translation disabled: Unable to determine device locale."
            )
            return InitializationResult(
                InitializationStatus.FAILED_NO_LOCALE,
                message = "Unable to determine device locale"
            )
        }
        
        // Get the language tag (e.g., "es-ES", "fr-FR", "en-US")
        // Then extract just the language code (e.g., "es", "fr", "en")
        val languageTag = deviceLocale.toLanguageTag()
        val deviceLanguage = languageTag.split("-")[0].lowercase()
        
        Log.debug(
            MessagingConstants.LOG_TAG,
            SELF_TAG,
            "Detected locale - App default: ${appLocale?.toLanguageTag()}, " +
            "System locale: ${systemLocale?.toLanguageTag()}, " +
            "Using: $languageTag, Language code: $deviceLanguage"
        )
        
        // Check if device language is English - no translation needed
        if (deviceLanguage == "en") {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Translation disabled: Device language is already English."
            )
            return InitializationResult(
                InitializationStatus.FAILED_ENGLISH_LOCALE,
                message = "Device language is already English"
            )
        }
        
        // Convert device language to ML Kit language code
        targetLanguageCode = try {
            TranslateLanguage.fromLanguageTag(deviceLanguage)
        } catch (e: IllegalArgumentException) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Translation disabled: Error converting language tag '$deviceLanguage': ${e.localizedMessage}"
            )
            return InitializationResult(
                InitializationStatus.FAILED_UNSUPPORTED_LANGUAGE,
                message = "Error converting language tag '$deviceLanguage': ${e.localizedMessage}"
            )
        }
        
        if (targetLanguageCode == null) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Translation disabled: Unsupported device language: $deviceLanguage"
            )
            return InitializationResult(
                InitializationStatus.FAILED_UNSUPPORTED_LANGUAGE,
                message = "Unsupported device language: $deviceLanguage"
            )
        }
        
        // Create translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguageCode!!)
            .build()
        
        translator = Translation.getClient(options)
        
        // Check if model is already downloaded (cached)
        // This avoids unnecessary download attempts and makes initialization faster
        Log.debug(
            MessagingConstants.LOG_TAG,
            SELF_TAG,
            "Starting model check/download for language: $targetLanguageCode"
        )
        
        return try {
            val result = runBlocking {
                try {
                    // Notify that we're checking the cache
                    progressCallback?.onCheckingCache()
                    
                    // First check if model is already available
                    Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Checking if model is downloaded..."
                    )
                    
                    val remoteModel = TranslateRemoteModel.Builder(targetLanguageCode!!).build()
                    val modelDownloadManager = RemoteModelManager.getInstance()
                    
                    val isModelDownloaded = try {
                        val result = withTimeout(10_000L) {
                            modelDownloadManager.isModelDownloaded(remoteModel).await()
                        }
                        Log.debug(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Model check completed: isDownloaded = $result"
                        )
                        result
                    } catch (e: Exception) {
                        Log.warning(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Error checking if model is downloaded: ${e.message}, ${e.javaClass.simpleName}"
                        )
                        false
                    }
                    
                    if (isModelDownloaded) {
                        Log.debug(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Translation model for language '$targetLanguageCode' is already cached"
                        )
                        isTranslationEnabled = true
                        
                        // Record access to update LRU cache
                        cacheManager.recordModelAccess(targetLanguageCode!!)
                        
                        InitializationResult(
                            InitializationStatus.SUCCESS_MODEL_CACHED,
                            languageCode = targetLanguageCode,
                            message = "Translation model found in cache"
                        )
                    } else {
                        // Model not cached, download it
                        Log.debug(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Translation model not found, starting download for language: $targetLanguageCode"
                        )
                        
                        // Clean up old models if cache limit reached
                        Log.debug(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Checking cache limit before download..."
                        )
                        val cleanupSuccess = cacheManager.cleanupOldModelsIfNeeded(targetLanguageCode!!)
                        if (!cleanupSuccess) {
                            Log.warning(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "Cache cleanup had issues, but proceeding with download"
                            )
                        }
                        
                        // Notify that download is starting
                        progressCallback?.onDownloadStarted(targetLanguageCode!!)
                        
                        val conditions = DownloadConditions.Builder()
                            .requireWifi()
                            .build()
                        
                        try {
                            withTimeout(MODEL_DOWNLOAD_TIMEOUT_MS) {
                                translator?.downloadModelIfNeeded(conditions)?.await()
                            }
                            Log.debug(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "Translation model downloaded successfully for language: $targetLanguageCode"
                            )
                            isTranslationEnabled = true
                            
                            // Record the newly downloaded model
                            cacheManager.recordModelAccess(targetLanguageCode!!)
                            
                            InitializationResult(
                                InitializationStatus.SUCCESS_MODEL_DOWNLOADED,
                                languageCode = targetLanguageCode,
                                message = "Translation model downloaded successfully"
                            )
                        } catch (downloadException: Exception) {
                            Log.warning(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "Model download failed: ${downloadException.message}, ${downloadException.javaClass.simpleName}"
                            )
                            isTranslationEnabled = false
                            InitializationResult(
                                InitializationStatus.FAILED_MODEL_DOWNLOAD,
                                languageCode = targetLanguageCode,
                                message = "Model download failed: ${downloadException.message}"
                            )
                        }
                    }
                } catch (innerException: Exception) {
                    Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Inner exception during initialization: ${innerException.message}, ${innerException.javaClass.simpleName}"
                    )
                    isTranslationEnabled = false
                    InitializationResult(
                        InitializationStatus.FAILED_EXCEPTION,
                        languageCode = targetLanguageCode,
                        message = "Exception during initialization: ${innerException.message}"
                    )
                }
            }
            
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Initialization completed: status = ${result.status}, isTranslationEnabled = $isTranslationEnabled"
            )
            result
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed during initialization: ${e.message}, ${e.javaClass.simpleName}, Stack: ${e.stackTraceToString()}"
            )
            isTranslationEnabled = false
            InitializationResult(
                InitializationStatus.FAILED_EXCEPTION,
                languageCode = targetLanguageCode,
                message = "Failed during initialization: ${e.message}"
            )
        }
    }
    
    /**
     * Translates the content in a PropositionItem from English to the device's native language.
     * Only translates the "content" field within itemData; all other fields remain unchanged.
     *
     * @param propositionItem the [PropositionItem] to translate
     * @return a new [PropositionItem] with translated content, or the original if
     *     translation fails, or null if input is null
     */
    fun translatePropositionItem(propositionItem: PropositionItem?): PropositionItem? {
        if (propositionItem == null || !isTranslationEnabled || translator == null) {
            return propositionItem
        }
        
        return try {
            val itemData = propositionItem.itemData
            if (itemData.isNullOrEmpty()) {
                return propositionItem
            }
            
            // Only translate the "content" field if it exists
            val contentValue = itemData["content"]
            if (contentValue == null) {
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "No 'content' field found in itemData, skipping translation"
                )
                return propositionItem
            }
            
            // Create a copy of itemData with translated content
            val translatedData = itemData.toMutableMap()
            translatedData["content"] = when (contentValue) {
                is String -> translateText(contentValue)
                is Map<*, *> -> translateMap(contentValue as Map<String, Any?>)
                is List<*> -> translateList(contentValue as List<Any?>)
                else -> contentValue // Keep as-is if not a translatable type
            }
            
            // Create a new PropositionItem with translated data
            PropositionItem(
                propositionItem.itemId,
                propositionItem.schema,
                translatedData
            )
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to translate PropositionItem: ${e.localizedMessage}"
            )
            propositionItem
        }
    }
    
    /**
     * Recursively translates all string values in a Map.
     *
     * @param map the map to translate
     * @return a new map with translated values
     */
    private fun translateMap(map: Map<String, Any?>): Map<String, Any?> {
        return map.mapValues { (_, value) ->
            when (value) {
                null -> null
                is String -> translateText(value)
                is Map<*, *> -> translateMap(value as Map<String, Any?>)
                is List<*> -> translateList(value as List<Any?>)
                else -> value // Keep other types as-is (numbers, booleans, etc.)
            }
        }
    }
    
    /**
     * Recursively translates all string values in a List.
     *
     * @param list the list to translate
     * @return a new list with translated values
     */
    private fun translateList(list: List<Any?>): List<Any?> {
        return list.map { item ->
            when (item) {
                null -> null
                is String -> translateText(item)
                is Map<*, *> -> translateMap(item as Map<String, Any?>)
                is List<*> -> translateList(item as List<Any?>)
                else -> item
            }
        }
    }
    
    /**
     * Translates a single text string, preserving HTML tags if present.
     *
     * @param text the text to translate
     * @return the translated text, or the original if translation fails
     */
    private fun translateText(text: String?): String? {
        if (text.isNullOrEmpty() || translator == null) {
            return text
        }
        
        // Skip translation for very short strings (likely not user-facing text)
        if (text.length < 2) {
            return text
        }
        
        // Check if text contains HTML tags
        return if (HTML_TAG_PATTERN.containsMatchIn(text)) {
            translateHtmlText(text)
        } else {
            translatePlainText(text)
        }
    }
    
    /**
     * Translates plain text without HTML tags.
     *
     * @param text the plain text to translate
     * @return the translated text, or the original if translation fails
     */
    private fun translatePlainText(text: String): String {
        return try {
            runBlocking {
                withTimeout(TRANSLATION_TIMEOUT_MS) {
                    translator?.translate(text)?.await() ?: text
                }
            }
        } catch (e: Exception) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to translate text: ${e.localizedMessage}"
            )
            text
        }
    }
    
    /**
     * Translates HTML text while preserving HTML tags and structure.
     * Only translates plain text segments without newlines.
     *
     * @param htmlText the HTML text to translate
     * @return the translated HTML text, or the original if translation fails
     */
    private fun translateHtmlText(htmlText: String): String {
        return try {
            val textSegments = mutableListOf<String>()
            val htmlSegments = mutableListOf<String>()
            
            var lastEnd = 0
            HTML_TAG_PATTERN.findAll(htmlText).forEach { matchResult ->
                // Add text before the tag
                if (matchResult.range.first > lastEnd) {
                    textSegments.add(htmlText.substring(lastEnd, matchResult.range.first))
                }
                
                // Add the HTML tag
                htmlSegments.add(matchResult.value)
                lastEnd = matchResult.range.last + 1
            }
            
            // Add remaining text after the last tag
            if (lastEnd < htmlText.length) {
                textSegments.add(htmlText.substring(lastEnd))
            }
            
            // Translate text segments (only plain text without newlines)
            val translatedSegments = textSegments.map { segment ->
                val trimmedSegment = segment.trim()
                if (trimmedSegment.isNotEmpty() && !trimmedSegment.contains('\n')) {
                    translatePlainText(trimmedSegment)
                } else {
                    segment // Preserve whitespace and text with newlines
                }
            }
            
            // Reconstruct HTML with translated text
            buildString {
                var textIndex = 0
                var htmlIndex = 0
                var lastEnd = 0
                
                HTML_TAG_PATTERN.findAll(htmlText).forEach { matchResult ->
                    if (matchResult.range.first > lastEnd && textIndex < translatedSegments.size) {
                        append(translatedSegments[textIndex++])
                    }
                    if (htmlIndex < htmlSegments.size) {
                        append(htmlSegments[htmlIndex++])
                    }
                    lastEnd = matchResult.range.last + 1
                }
                
                if (textIndex < translatedSegments.size) {
                    append(translatedSegments[textIndex])
                }
            }
        } catch (e: Exception) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to translate HTML text: ${e.localizedMessage}"
            )
            htmlText
        }
    }
    
    /**
     * Releases resources used by the translator. Should be called when translation is no longer
     * needed.
     */
    fun cleanup() {
        translator?.close()
        translator = null
        isTranslationEnabled = false
    }
    
    /**
     * Checks if translation is currently enabled and ready.
     *
     * @return `true` if translation is enabled, `false` otherwise
     */
    fun isTranslationEnabled(): Boolean = isTranslationEnabled
    
    /**
     * Gets the target language code being used for translation.
     *
     * @return the target language code, or `null` if translation is not enabled
     */
    fun getTargetLanguageCode(): String? = targetLanguageCode
}
