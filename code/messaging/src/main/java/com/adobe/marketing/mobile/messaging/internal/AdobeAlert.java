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

package com.adobe.marketing.mobile.messaging.internal;

import com.adobe.marketing.mobile.util.StringUtils;

class AdobeAlert {
    // Optional, plain-text title of the message
    private String title;
    // Optional, plain-text body of the message
    private String message;
    // Required, text to be displayed on the default button
    private String defaultButton;
    // Optional, url to redirect to when interacting with the default button
    private String defaultButtonUrl;
    // Optional, text to be displayed on the cancel button
    private String cancelButton;
    // Optional, url to redirect to when interacting with the cancel button
    private String cancelButtonUrl;
    // Required, represents whether the alert will be presented as a dialog or an action sheet on the bottom of the screen
    private String style;

    /**
     * Private constructor.
     * <p>
     * Use {@link Builder} to create {@link AdobeAlert} object.
     */
    private AdobeAlert() {
    }

    /**
     * {@code AdobeAlert} Builder.
     */
    public static class Builder {
        private boolean didBuild;
        final private AdobeAlert adobeAlert;

        /**
         * Builder constructor with required {@code AdobeAlert} attributes as parameters.
         * <p>
         * It sets default values for the remaining {@link AdobeAlert} attributes.
         *
         * @param defaultButton required {@link String} text to be displayed on the default button
         * @param style         required {@link String} representing whether the alert will be presented as a dialog or an action sheet on the bottom of the screen
         */
        public Builder(final String defaultButton, final String style) {
            adobeAlert = new AdobeAlert();
            adobeAlert.defaultButton = StringUtils.isNullOrEmpty(defaultButton) ? "" : defaultButton;
            adobeAlert.style = StringUtils.isNullOrEmpty(style) ? "" : style;
            adobeAlert.title = "";
            adobeAlert.message = "";
            adobeAlert.defaultButtonUrl = "";
            adobeAlert.cancelButton = "";
            adobeAlert.cancelButtonUrl = "";
            didBuild = false;
        }

        /**
         * Sets the title text for this {@code AdobeAlert}.
         *
         * @param title {@link String} containing the plain-text title of the message
         * @return this AdobeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setTitle(final String title) {
            throwIfAlreadyBuilt();

            adobeAlert.title = title;
            return this;
        }

        /**
         * Sets the message body text for this {@code AdobeAlert}.
         *
         * @param message {@link String} containing the plain-text body of the message
         * @return this AdobeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setMessage(final String message) {
            throwIfAlreadyBuilt();

            adobeAlert.message = message;
            return this;
        }

        /**
         * Sets the default button url for this {@code AdobeAlert}.
         *
         * @param defaultButtonUrl {@link String} containing the url to redirect to when interacting with the default button
         * @return this AdobeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setDefaultButtonUrl(final String defaultButtonUrl) {
            throwIfAlreadyBuilt();

            adobeAlert.defaultButtonUrl = defaultButtonUrl;
            return this;
        }

        /**
         * Sets the cancel button text for this {@code AdobeAlert}.
         *
         * @param cancelButton {@link String} containing the text to be displayed on the cancel button
         * @return this AdobeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setCancelButton(final String cancelButton) {
            throwIfAlreadyBuilt();

            adobeAlert.cancelButton = cancelButton;
            return this;
        }

        /**
         * Sets the cancel button url for this {@code AdobeAlert}.
         *
         * @param cancelButtonUrl {@link String} containing the url to redirect to when interacting with the cancel button
         * @return this AdobeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setCancelButtonUrl(final String cancelButtonUrl) {
            throwIfAlreadyBuilt();

            adobeAlert.cancelButtonUrl = cancelButtonUrl;
            return this;
        }

        /**
         * Builds and returns the {@code AdobeAlert} object.
         *
         * @return {@link AdobeAlert} object or null.
         */
        public AdobeAlert build() {
            // default button text and alert style are required. additionally, alert style must be "alert" or "actionSheet".
            if (StringUtils.isNullOrEmpty(adobeAlert.defaultButton)
                    || StringUtils.isNullOrEmpty(adobeAlert.style)
                    || (!adobeAlert.style.equals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ALERT) && !adobeAlert.style.equals(MessagingConstants.EventDataKeys.RulesEngine.ALERT_STYLE_ACTION_SHEET))) {
                return null;
            }

            throwIfAlreadyBuilt();
            didBuild = true;

            return adobeAlert;
        }

        private void throwIfAlreadyBuilt() {
            if (didBuild) {
                throw new UnsupportedOperationException("Attempted to call methods on AdobeAlert.Builder after build() was invoked.");
            }
        }
    }

    /**
     * Gets the {@code AdobeAlert} title text.
     *
     * @return {@link String} containing the {@link AdobeAlert} title text.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the {@code AdobeAlert} message body text.
     *
     * @return {@link String} containing the {@link AdobeAlert} message body text.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the {@code AdobeAlert} default button text.
     *
     * @return {@link String} containing the {@link AdobeAlert} default button text.
     */
    public String getDefaultButton() {
        return defaultButton;
    }

    /**
     * Gets the {@code AdobeAlert} default button url.
     *
     * @return {@link String} containing the {@link AdobeAlert} default button url.
     */
    public String getDefaultButtonUrl() {
        return defaultButtonUrl;
    }

    /**
     * Gets the {@code AdobeAlert} cancel button text.
     *
     * @return {@link String} containing the {@link AdobeAlert} cancel button text.
     */
    public String getCancelButton() {
        return cancelButton;
    }

    /**
     * Gets the {@code AdobeAlert} cancel button url.
     *
     * @return {@link String} containing the {@link AdobeAlert} cancel button url.
     */
    public String getCancelButtonUrl() {
        return cancelButtonUrl;
    }

    /**
     * Gets the {@code AdobeAlert} style.
     *
     * @return {@link String} containing the {@link AdobeAlert} style.
     */
    public String getStyle() {
        return style;
    }
}
