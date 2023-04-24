/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messagingsample

import android.app.Application
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity
import com.google.firebase.messaging.FirebaseMessaging

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        Messaging.registerExtension()
        Identity.registerExtension()
        Edge.registerExtension()
        Assurance.registerExtension()
        //Assurance.startSession("ryan://?adb_validation_sessionid=d1e7b533-1af9-463b-835d-4b4a028e8cd2")

        MobileCore.start {
            // Necessary property id which has the edge configuration id needed by aep sdk
            MobileCore.configureWithAppID("3149c49c3910/4f6b2fbf2986/launch-7d78a5fd1de3-development")
            MobileCore.lifecycleStart(null)
        }


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // Log and toast
            if (task.isSuccessful) {
                // Get new FCM registration token
                val token = task.result
                print("MessagingApplication Firebase token :: $token")
                // Syncing the push token with experience platform
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}