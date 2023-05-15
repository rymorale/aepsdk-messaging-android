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

package com.adobe.marketing.mobile.messaging.internal;

import com.adobe.marketing.mobile.AlertMessage;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AlertListener;
import com.adobe.marketing.mobile.services.ui.NativeAlert;
import com.adobe.marketing.mobile.services.ui.UIError;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the Messaging extension implementation of {@link AlertListener}.
 */
public class MessagingAlertListener implements AlertListener {
    private final static String SELF_TAG = "MessagingAlertListener";

    private final NativeAlert nativeAlert;

    public MessagingAlertListener(final AlertMessage alertMessage) {
        this.nativeAlert = alertMessage;
    }

    /**
     * Invoked when the alert message is displayed.
     */
    @Override
    public void onShow() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Alert message shown.");
    }

    /**
     * Invoked when the alert message is dismissed.
     */
    @Override
    public void onDismiss() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Alert message dismissed.");
    }

    /**
     * Invoked when the alert message failed to be displayed.
     */
    @Override
    public void onError(UIError error) {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Alert message failed to show: %s", error.toString());
    }

    /**
     * Invoked when a {@link com.adobe.marketing.mobile.services.ui.NativeAlert} is attempting to load a URL.
     *
     * @param isPositiveResponse {@code boolean} if true then a positive alert response triggered this function call
     * @return true if the SDK wants to handle the URL
     */
    public void handleClickThroughUrl(final boolean isPositiveResponse) {
        String urlString;
        if (isPositiveResponse) {
            urlString = nativeAlert.getDefaultButtonUrl();
        } else {
            urlString = nativeAlert.getCancelButtonUrl();
        }

        if (StringUtils.isNullOrEmpty(urlString)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Cannot process provided URL string, it is null or empty.");
            return;
        }

        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "attempting to load url (%s)", urlString);

        final URI uri;

        try {
            uri = new URI(urlString);
        } catch (final URISyntaxException ex) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message URI found (%s), exception is: %s.", urlString, ex.getMessage());
            return;
        }


//        final String messageScheme = uri.getScheme();
//
//        // Quick bail out if scheme is not "adbinapp"
//        if (messageScheme == null || !messageScheme.equals(MessagingConstants.QueryParameters.ADOBE_INAPP)) {
//            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid message scheme found in URI. (%s)", urlString);
//            return;
//        }

        // Handle query parameters
        final String decodedQueryString = uri.getQuery();
        final Map<String, String> messageData = extractQueryParameters(decodedQueryString);
        if (!MapUtils.isNullOrEmpty(messageData)) {
            // handle optional tracking
            final String interaction = messageData.remove(MessagingConstants.QueryParameters.INTERACTION);
            if (!StringUtils.isNullOrEmpty(interaction)) {

                // ensure we have the MessagingExtension class available for tracking
//                final Object messagingExtension = message.getParent();
//                if (messagingExtension != null) {
//                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Tracking message interaction (%s)", interaction);
//                    message.track(interaction, MessagingEdgeEventType.IN_APP_INTERACT);
//                }
            }
        }

        openUrl(urlString);
    }

    /**
     * Open the passed in url using the {@link UIService}.
     *
     * @param url {@link String} containing the deeplink or url to be loaded
     */
    void openUrl(final String url) {
        if (StringUtils.isNullOrEmpty(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Will not openURL, url is null or empty.");
            return;
        }

        // pass the url to the ui service
        final UIService uiService = ServiceProvider.getInstance().getUIService();
        if (uiService == null || !uiService.showUrl(url)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Could not open URL (%s)", url);
        }
    }

    private static Map<String, String> extractQueryParameters(final String queryString) {
        if (StringUtils.isNullOrEmpty(queryString)) {
            return null;
        }

        final Map<String, String> parameters = new HashMap<String, String>();
        final String[] paramArray = queryString.split("&");

        for (String currentParam : paramArray) {
            // quick out in case this entry is null or empty string
            if (StringUtils.isNullOrEmpty(currentParam)) {
                continue;
            }

            final String[] currentParamArray = currentParam.split("=", 2);

            if (currentParamArray.length != 2 ||
                    (currentParamArray[0].isEmpty() || currentParamArray[1].isEmpty())) {
                continue;
            }

            final String key = currentParamArray[0];
            final String value = currentParamArray[1];
            parameters.put(key, value);
        }

        return parameters;
    }

    @Override
    public void onPositiveResponse() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Alert message default button pressed.");
        handleClickThroughUrl(true);
    }

    @Override
    public void onNegativeResponse() {
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Alert message cancel button pressed.");
        handleClickThroughUrl(false);
    }
}
