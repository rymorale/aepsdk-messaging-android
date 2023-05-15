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

import com.adobe.marketing.mobile.messaging.internal.MessagingAlertListener;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AlertListener;
import com.adobe.marketing.mobile.services.ui.NativeAlert;
import com.adobe.marketing.mobile.util.StringUtils;

public class AlertMessage implements NativeAlert {
    public static final String ALERT_STYLE_ALERT = "alert";
    public static final String ALERT_STYLE_ACTION_SHEET = "actionSheet";

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
    // Required, parent object which created this alert message
    private Object parent;

    private AlertListener alertListener;

    /**
     * Private constructor.
     * <p>
     * Use {@link Builder} to create {@link AlertMessage} object.
     */
    private AlertMessage() {
    }

    @Override
    public void dismiss() {
        // TODO: maybe implement? or no-op...
    }

    @Override
    public void show() {
        alertListener = new MessagingAlertListener(this);
        ServiceProvider.getInstance().getUIService().showNativeAlert(this, alertListener);
    }

    /**
     * {@code NativeAlert} Builder.
     */
    public static class Builder {
        private boolean didBuild;
        final private AlertMessage alertMessage;

        /**
         * Builder constructor with required {@code NativeAlert} attributes as parameters.
         * <p>
         * It sets default values for the remaining {@link AlertMessage} attributes.
         *
         * @param parent        required {@link Object} which created this {@link AlertMessage}
         * @param defaultButton required {@link String} text to be displayed on the default button
         * @param style         required {@link String} representing whether the alert will be presented as a dialog or an action sheet on the bottom of the screen
         */
        public Builder(final Object parent, final String defaultButton, final String style) {
            alertMessage = new AlertMessage();
            alertMessage.parent = parent;
            alertMessage.defaultButton = StringUtils.isNullOrEmpty(defaultButton) ? "" : defaultButton;
            alertMessage.style = StringUtils.isNullOrEmpty(style) ? "" : style;
            alertMessage.title = "";
            alertMessage.message = "";
            alertMessage.defaultButtonUrl = "";
            alertMessage.cancelButton = "";
            alertMessage.cancelButtonUrl = "";
            didBuild = false;
        }

        /**
         * Sets the title text for this {@code NativeAlert}.
         *
         * @param title {@link String} containing the plain-text title of the message
         * @return this NativeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setTitle(final String title) {
            throwIfAlreadyBuilt();

            alertMessage.title = title;
            return this;
        }

        /**
         * Sets the message body text for this {@code NativeAlert}.
         *
         * @param message {@link String} containing the plain-text body of the message
         * @return this NativeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setMessage(final String message) {
            throwIfAlreadyBuilt();

            alertMessage.message = message;
            return this;
        }

        /**
         * Sets the default button url for this {@code NativeAlert}.
         *
         * @param defaultButtonUrl {@link String} containing the url to redirect to when interacting with the default button
         * @return this NativeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setDefaultButtonUrl(final String defaultButtonUrl) {
            throwIfAlreadyBuilt();

            alertMessage.defaultButtonUrl = defaultButtonUrl;
            return this;
        }

        /**
         * Sets the cancel button text for this {@code NativeAlert}.
         *
         * @param cancelButton {@link String} containing the text to be displayed on the cancel button
         * @return this NativeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setCancelButton(final String cancelButton) {
            throwIfAlreadyBuilt();

            alertMessage.cancelButton = cancelButton;
            return this;
        }

        /**
         * Sets the cancel button url for this {@code NativeAlert}.
         *
         * @param cancelButtonUrl {@link String} containing the url to redirect to when interacting with the cancel button
         * @return this NativeAlert {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setCancelButtonUrl(final String cancelButtonUrl) {
            throwIfAlreadyBuilt();

            alertMessage.cancelButtonUrl = cancelButtonUrl;
            return this;
        }

        /**
         * Builds and returns the {@code NativeAlert} object.
         *
         * @return {@link AlertMessage} object or null.
         */
        public AlertMessage build() {
            // default button text and alert style are required. additionally, alert style must be "alert" or "actionSheet".
            if (StringUtils.isNullOrEmpty(alertMessage.defaultButton)
                    || StringUtils.isNullOrEmpty(alertMessage.style)
                    || (!alertMessage.style.equals(ALERT_STYLE_ALERT) && !alertMessage.style.equals(ALERT_STYLE_ACTION_SHEET))) {
                return null;
            }

            throwIfAlreadyBuilt();
            didBuild = true;

            return alertMessage;
        }

        private void throwIfAlreadyBuilt() {
            if (didBuild) {
                throw new UnsupportedOperationException("Attempted to call methods on NativeAlert.Builder after build() was invoked.");
            }
        }
    }

    /**
     * Gets the {@code NativeAlert} title text.
     *
     * @return {@link String} containing the {@link AlertMessage} title text.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the {@code NativeAlert} message body text.
     *
     * @return {@link String} containing the {@link AlertMessage} message body text.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the {@code NativeAlert} default button text.
     *
     * @return {@link String} containing the {@link AlertMessage} default button text.
     */
    public String getDefaultButton() {
        return defaultButton;
    }

    /**
     * Gets the {@code NativeAlert} default button url.
     *
     * @return {@link String} containing the {@link AlertMessage} default button url.
     */
    public String getDefaultButtonUrl() {
        return defaultButtonUrl;
    }

    /**
     * Gets the {@code NativeAlert} cancel button text.
     *
     * @return {@link String} containing the {@link AlertMessage} cancel button text.
     */
    public String getCancelButton() {
        return cancelButton;
    }

    /**
     * Gets the {@code NativeAlert} cancel button url.
     *
     * @return {@link String} containing the {@link AlertMessage} cancel button url.
     */
    public String getCancelButtonUrl() {
        return cancelButtonUrl;
    }

    /**
     * Gets the {@code NativeAlert} style.
     *
     * @return {@link String} containing the {@link AlertMessage} style.
     */
    public String getStyle() {
        return style;
    }

    /**
     * Returns the {@link Object} which created this {@link AlertMessage} object.
     */
    @Override
    public Object getParent() {
        return parent;
    }
}