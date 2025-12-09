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

/**
 * Public callback interface for translation initialization status updates.
 */
interface TranslationStatusCallback {
    /**
     * Called when translation initialization starts.
     */
    fun onTranslationInitializationStarted()
    
    /**
     * Called when checking if the model is cached.
     */
    fun onCheckingModelCache()
    
    /**
     * Called when the translation model is found in the cache.
     * @param languageCode the target language code
     */
    fun onModelFoundInCache(languageCode: String)
    
    /**
     * Called when the model download starts.
     * @param languageCode the target language code
     */
    fun onModelDownloadStarted(languageCode: String)
    
    /**
     * Called when the translation model is successfully downloaded.
     * @param languageCode the target language code
     */
    fun onModelDownloadedSuccessfully(languageCode: String)
    
    /**
     * Called when translation initialization fails.
     * @param reason the reason for failure
     */
    fun onTranslationInitializationFailed(reason: String)
}

