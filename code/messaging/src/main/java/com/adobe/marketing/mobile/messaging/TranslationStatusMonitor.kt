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
 * Public helper class that allows applications to monitor translation status.
 * This provides a bridge between the internal translation manager and the application UI.
 */
object TranslationStatusMonitor {
    
    @Volatile
    private var callback: TranslationStatusCallback? = null
    
    /**
     * Sets the callback to receive translation status updates.
     * 
     * @param statusCallback the callback to receive status updates, or null to remove
     */
    @JvmStatic
    fun setStatusCallback(statusCallback: TranslationStatusCallback?) {
        callback = statusCallback
    }
    
    /**
     * Gets the current status callback.
     * This is used internally by the SDK to forward status updates.
     */
    @JvmStatic
    fun getStatusCallback(): TranslationStatusCallback? {
        return callback
    }
}

