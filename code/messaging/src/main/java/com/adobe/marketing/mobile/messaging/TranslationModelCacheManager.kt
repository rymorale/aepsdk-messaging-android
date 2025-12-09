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

import android.content.Context
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.tasks.await

/**
 * Manages the cache of translation models with a maximum size limit.
 * Uses LRU (Least Recently Used) eviction strategy to remove old models when limit is reached.
 */
internal class TranslationModelCacheManager {
    
    companion object {
        private const val SELF_TAG = "TranslationModelCacheManager"
        private const val PREFS_NAME = "aep_translation_model_cache"
        private const val MAX_CACHED_MODELS = 5
        
        // SharedPreferences key format: "model_<languageCode>" -> timestamp
        private const val MODEL_PREFIX = "model_"
    }
    
    private val context: Context?
        get() = ServiceProvider.getInstance().appContextService?.applicationContext
    
    /**
     * Records that a model for the given language code was accessed (downloaded or used).
     * Updates the timestamp to mark it as recently used.
     * 
     * @param languageCode the language code of the model
     */
    fun recordModelAccess(languageCode: String) {
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()?.apply {
                putLong("$MODEL_PREFIX$languageCode", System.currentTimeMillis())
                apply()
            }
            
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Recorded access for translation model: $languageCode"
            )
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to record model access for $languageCode: ${e.message}"
            )
        }
    }
    
    /**
     * Checks if cache is at or over the limit, and if so, removes the least recently used models.
     * Should be called before downloading a new model.
     * 
     * @param newLanguageCode the language code of the model about to be downloaded
     * @return true if cleanup was successful or not needed, false if cleanup failed
     */
    suspend fun cleanupOldModelsIfNeeded(newLanguageCode: String): Boolean {
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?: return false
            
            // Get all cached models with their timestamps
            val cachedModels = mutableMapOf<String, Long>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(MODEL_PREFIX) && value is Long) {
                    val languageCode = key.removePrefix(MODEL_PREFIX)
                    cachedModels[languageCode] = value
                }
            }
            
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Current cached models count: ${cachedModels.size}, max: $MAX_CACHED_MODELS"
            )
            
            // If we're at or over the limit, remove oldest models
            // We remove enough to make room for the new model
            if (cachedModels.size >= MAX_CACHED_MODELS) {
                // Sort by timestamp (oldest first)
                val sortedModels = cachedModels.entries
                    .sortedBy { it.value }
                    .map { it.key }
                
                // Calculate how many to remove (at least 1 to make room)
                val modelsToRemove = (cachedModels.size - MAX_CACHED_MODELS + 1).coerceAtLeast(1)
                
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cache limit reached. Removing $modelsToRemove oldest model(s)"
                )
                
                // Remove the oldest models
                val modelManager = RemoteModelManager.getInstance()
                var removedCount = 0
                
                for (i in 0 until modelsToRemove.coerceAtMost(sortedModels.size)) {
                    val languageToRemove = sortedModels[i]
                    
                    // Don't remove the model we're about to use
                    if (languageToRemove == newLanguageCode) {
                        continue
                    }
                    
                    try {
                        val model = TranslateRemoteModel.Builder(languageToRemove).build()
                        modelManager.deleteDownloadedModel(model).await()
                        
                        // Remove from preferences
                        prefs.edit().remove("$MODEL_PREFIX$languageToRemove").apply()
                        
                        removedCount++
                        Log.debug(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Removed old translation model: $languageToRemove (${i + 1}/$modelsToRemove)"
                        )
                    } catch (e: Exception) {
                        Log.warning(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Failed to remove model $languageToRemove: ${e.message}"
                        )
                    }
                }
                
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Successfully removed $removedCount old model(s)"
                )
            } else {
                Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cache within limit (${cachedModels.size}/$MAX_CACHED_MODELS), no cleanup needed"
                )
            }
            
            return true
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to cleanup old models: ${e.message}"
            )
            return false
        }
    }
    
    /**
     * Removes a specific model from the cache tracking.
     * Note: This doesn't delete the actual model file, just removes it from tracking.
     * 
     * @param languageCode the language code to remove
     */
    fun removeModelFromTracking(languageCode: String) {
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()?.remove("$MODEL_PREFIX$languageCode")?.apply()
            
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Removed model from tracking: $languageCode"
            )
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to remove model from tracking: ${e.message}"
            )
        }
    }
    
    /**
     * Gets the list of currently tracked models with their access timestamps.
     * Useful for debugging and monitoring.
     * 
     * @return Map of language code to last access timestamp
     */
    fun getTrackedModels(): Map<String, Long> {
        return try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?: return emptyMap()
            
            val models = mutableMapOf<String, Long>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(MODEL_PREFIX) && value is Long) {
                    val languageCode = key.removePrefix(MODEL_PREFIX)
                    models[languageCode] = value
                }
            }
            models
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to get tracked models: ${e.message}"
            )
            emptyMap()
        }
    }
    
    /**
     * Clears all cache tracking data.
     * Note: This doesn't delete the actual model files.
     */
    fun clearAllTracking() {
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()?.clear()?.apply()
            
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Cleared all model cache tracking"
            )
        } catch (e: Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to clear cache tracking: ${e.message}"
            )
        }
    }
}

