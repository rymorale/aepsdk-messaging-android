/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.List;

/**
 * Class for building push notification.
 * <p>
 *     The build method in this class takes {@link MessagingPushPayload} received from the push notification and builds the notification.
 *     This class is used internally by AdobeMessagingService to build the push notification.
 */
class MessagingPushBuilder {

    private static final String SELF_TAG = "PushNotificationBuilder";
    private static final String DEFAULT_CHANNEL_ID = "AdobeAJOPushChannel";
    // When no channel name is received from the push notification, this default channel name is used.
    // This will appear in the notification settings for the app.
    private static final String DEFAULT_CHANNEL_NAME = "General";

    /**
     * Builds a notification for the received payload.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @return the notification
     */
    @NonNull
    static Notification build(final MessagingPushPayload payload,
                       final Context context){
        final String channelId = createChannelAndGetChannelID(payload, context);

        // Create the notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setContentTitle(payload.getTitle());
        builder.setContentText(payload.getBody());
        builder.setNumber(payload.getBadgeCount());
        builder.setPriority(payload.getNotificationPriority());
        builder.setAutoCancel(true);

        setLargeIcon(builder, payload);
        setSmallIcon(builder, payload, context); // Small Icon must be present, otherwise the notification will not be displayed.
        addActionButtons(builder, payload, context); // Add action buttons if any
        setSound(builder, payload, context);
        setNotificationClickAction(builder, payload, context);
        setNotificationDeleteAction(builder, payload, context);

        return builder.build();
    }

    /**
     * Creates a channel if it does not exist and returns the channel ID.
     * If a channel ID is received from the payload and if channel exists for the channel ID, the same channel ID is returned.
     * If a channel ID is received from the payload and if channel does not exist for the channel ID, Messaging extension's default channel is used.
     * If no channel ID is received from the payload, Messaging extension's default channel is used.
     * For Android versions below O, no channel is created. Just return the obtained channel ID.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @return the channel ID
     */
    @NonNull
    private static String createChannelAndGetChannelID(final MessagingPushPayload payload,
                                                final Context context)  {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // For Android versions below O, no channel is created. Just return the obtained channel ID.
            return payload.getChannelId() == null ? DEFAULT_CHANNEL_ID : payload.getChannelId();
        } else {
            // For Android versions O and above, create a channel if it does not exist and return the channel ID.
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            final String channelIdFromPayload = payload.getChannelId();

            // if a channel from the payload
            if (channelIdFromPayload != null && notificationManager.getNotificationChannel(channelIdFromPayload) != null) {
                Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "Channel exists for channel ID: " + channelIdFromPayload + ". Using the same for push notification.");
                return channelIdFromPayload;
            } else {
                Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "Channel does not exist for channel ID obtained from payload ( " + channelIdFromPayload + "). Using the Messaging Extension's default channel.");
            }

            // Use the default channel ID if the channel ID from the payload is null or if a channel does not exist for the channel ID from the payload.
            final String channelId = DEFAULT_CHANNEL_ID;
            if (notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) {
                Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "Channel already exists for the default channel ID: " + channelId);
                return DEFAULT_CHANNEL_ID;
            } else {
                Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "Creating a new channel for the default channel ID: " + channelId + ".");
                final NotificationChannel channel = new NotificationChannel(channelId, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            return channelId;
        }
    }

    /**
     * Sets the small icon for the notification.
     * If a small icon is received from the payload, the same is used.
     * If a small icon is not received from the payload, we use the icon set using MobileCore.setSmallIcon().
     * If a small icon is not set using MobileCore.setSmallIcon(), we use the default small icon of the application.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @param builder the notification builder
     */
    private static void setSmallIcon(final NotificationCompat.Builder builder,
                              final MessagingPushPayload payload,
                              final Context context) {
        final int iconFromPayload = MessagingPushUtils.getSmallIconWithResourceName(payload.getIcon(), context);
        final int iconFromMobileCore =  -1 ;   //MobileCore.getSmallIconResourceId();

        if (isValidIcon(iconFromPayload)) {
            builder.setSmallIcon(iconFromPayload);
        } else if (isValidIcon(iconFromMobileCore)) {
            builder.setSmallIcon(iconFromMobileCore);
        } else {
            final int iconFromApp = MessagingPushUtils.getDefaultAppIcon(context);
            if (isValidIcon(iconFromApp)) {
                builder.setSmallIcon(iconFromApp);
            } else {
                Log.warning(MessagingPushConstants.LOG_TAG, SELF_TAG, "No valid small icon found. Notification will not be displayed.");
            }
        }
    }

    /**
     * Sets the sound for the notification.
     * If a sound is received from the payload, the same is used.
     * If a sound is not received from the payload, the default sound is used
<<<<<<< HEAD
     * The sound name from the payload should also include the format of the sound file. eg: sound.mp3
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
=======
>>>>>>> 1e31646 (Auto track notifications - Dispactch launch intents and make use of Core Lifecycle listner's)
     * @param notificationBuilder the notification builder
     */
    private static void setSound(final NotificationCompat.Builder notificationBuilder,
                          final MessagingPushPayload payload,
                          final Context context) {
        if(!StringUtils.isNullOrEmpty(payload.getSound())) {
            notificationBuilder.setSound(MessagingPushUtils.getSoundUriForResourceName(payload.getSound(), context));
            return;
        }
        notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    /**
     * Sets the large icon for the notification.
     * If a large icon url is received from the payload, the image is downloaded and the notification style is set to BigPictureStyle.
     * If large icon url is not received from the payload, default style is used for the notification.
     *
     * @param notificationBuilder the notification builder
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     */
    private static void setLargeIcon(final NotificationCompat.Builder notificationBuilder,
                              final MessagingPushPayload payload) {
        // Quick bail out if there is no image url
        if (StringUtils.isNullOrEmpty(payload.getImageUrl())) return;
        Bitmap bitmap = MessagingPushUtils.download(payload.getImageUrl());

        // Bail out if the download fails
        if (bitmap == null) return;
        notificationBuilder.setLargeIcon(bitmap);
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        bigPictureStyle.bigLargeIcon(bitmap);
        notificationBuilder.setStyle(bigPictureStyle);
    }

    private static void setNotificationClickAction(final NotificationCompat.Builder notificationBuilder,
                                            final MessagingPushPayload payload,
                                            final Context context) {
        if (payload.getActionType() == MessagingPushPayload.ActionType.DEEPLINK || payload.getActionType() == MessagingPushPayload.ActionType.WEBURL) {
            notificationBuilder.setContentIntent(createDeepLinkIntent(payload, context,payload.getActionUri(), MessagingPushConstants.Tracking.Values.PUSH_TRACKING_APPLICATION_OPENED));
        } else {
            notificationBuilder.setContentIntent(createOpenAppIntent(payload,context));
        }
    }

    private static void addActionButtons(final NotificationCompat.Builder builder,
                                  final MessagingPushPayload payload,
                                  final Context context) {
        final List<MessagingPushPayload.ActionButton> actionButtons = payload.getActionButtons();
        if (actionButtons == null || actionButtons.isEmpty()) {
            return;
        }

        for(MessagingPushPayload.ActionButton eachButton : actionButtons) {
            final PendingIntent pendingIntent;
            if (eachButton.getType() == MessagingPushPayload.ActionType.DEEPLINK|| eachButton.getType() == MessagingPushPayload.ActionType.WEBURL) {
                pendingIntent = createDeepLinkIntent(payload, context, eachButton.getLink(), MessagingPushConstants.Tracking.Values.PUSH_TRACKING_CUSTOM_ACTION);
            } else {
                pendingIntent = createOpenAppIntent(payload,context);
            }
            builder.addAction(0, eachButton.getLabel(), pendingIntent);
        }
    }

    private static PendingIntent createOpenAppIntent(final MessagingPushPayload payload,
                                              final Context context) {
        final Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Messaging.addPushTrackingDetails(launchIntent,payload.getMessageId(), payload.getData());
        launchIntent.putExtra("AJOPushInteraction", true);
        launchIntent.putExtra(MessagingPushConstants.Tracking.Keys.EVENT_TYPE, MessagingPushConstants.Tracking.Values.PUSH_TRACKING_APPLICATION_OPENED);
        launchIntent.putExtra(MessagingPushConstants.Tracking.Keys.APPLICATION_OPENED, true);
        return PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createDeepLinkIntent(final MessagingPushPayload payload,
                                               final Context context,
                                               final String actionUri,
                                               final String eventType) {
        if(StringUtils.isNullOrEmpty(actionUri)) {
            return createOpenAppIntent(payload,context);
        }
        final Intent deeplinkIntent = new Intent(Intent.ACTION_VIEW);
        deeplinkIntent.putExtra("AJOPushInteraction", true);
        deeplinkIntent.putExtra(MessagingPushConstants.Tracking.Keys.EVENT_TYPE, eventType);
        deeplinkIntent.putExtra(MessagingPushConstants.Tracking.Keys.APPLICATION_OPENED, true);
        deeplinkIntent.setData(Uri.parse(actionUri));
        Messaging.addPushTrackingDetails(deeplinkIntent,payload.getMessageId(), payload.getData());
        return PendingIntent.getActivity(context, 0, deeplinkIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void setNotificationDeleteAction(final NotificationCompat.Builder builder,
                                             final MessagingPushPayload payload,
                                             final Context context) {
        final Intent deleteIntent = new Intent(context, MessagingDeleteIntentReceiver.class);
        Messaging.addPushTrackingDetails(deleteIntent,payload.getMessageId(), payload.getData());
        final PendingIntent intent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(intent);
    }

    /**
     * Checks if the icon is valid.
     * @param icon the icon to be checked
     * @return true if the icon is valid, false otherwise
     */
    private static boolean isValidIcon(final int icon) {
        return icon > 0;
    }
}
