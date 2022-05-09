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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MessagingPushPayload
import com.adobe.marketing.mobile.MobileCore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        print("MessagingApplication Firebase token :: $token")
        MobileCore.setPushIdentifier(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // AEPMessaging handling of the push payload. If shouldHandleTracking is true then the AEPMessaging extension will handle push notification interaction tracking automatically.
        Messaging.handlePushNotificationWithRemoteMessage(message, true)
        // app handling of the push payload
        // createNotification(message)
    }

    private fun createNotification(message: RemoteMessage) {
        val payload = MessagingPushPayload(message)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelName = "some channel name"
            channelId = if (payload.channelId != null) payload.channelId else channelId
            val channel = NotificationChannel(
                channelId,
                channelName,
                getImportance(payload.notificationPriority)
            ).apply {
                description = "Settings for push notification"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle(payload.title)
            setContentTitle(payload.body)

            priority = payload.notificationPriority
            setContentIntent(
                PendingIntent.getActivity(
                    this@NotificationService,
                    0,
                    Intent(this@NotificationService, MainActivity::class.java).apply {
                        Messaging.addPushTrackingDetails(this, message.messageId, message.data)
                    },
                    0
                )
            )
            setDeleteIntent(
                PendingIntent.getBroadcast(
                    this@NotificationService,
                    0,
                    Intent(
                        this@NotificationService.applicationContext,
                        NotificationDeleteReceiver::class.java
                    ).apply {
                        Messaging.addPushTrackingDetails(this, message.messageId, message.data)
                    },
                    0
                )
            )
            setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun getImportance(priority: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when (priority) {
                Notification.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
                Notification.PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
                Notification.PRIORITY_HIGH -> NotificationManager.IMPORTANCE_HIGH
                Notification.PRIORITY_MAX -> NotificationManager.IMPORTANCE_MAX
                Notification.PRIORITY_DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                else -> NotificationManager.IMPORTANCE_NONE
            }
        } else NotificationManager.IMPORTANCE_DEFAULT
    }

    companion object {
        @JvmField
        val NOTIFICATION_ID = 0x12E45
        var channelId = "messaging_notification_channel"
        const val NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED_ACTION"
    }
}