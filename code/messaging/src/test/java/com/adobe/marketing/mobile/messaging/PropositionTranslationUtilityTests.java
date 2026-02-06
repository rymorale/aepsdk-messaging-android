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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PropositionTranslationUtilityTests {

    @Mock private ServiceProvider mockServiceProvider;
    @Mock private DeviceInforming mockDeviceInfoService;

    private PropositionTranslationUtility translationUtility;

    @Before
    public void setup() {
        translationUtility = new PropositionTranslationUtility();
    }

    @After
    public void tearDown() {
        if (translationUtility != null) {
            translationUtility.cleanup();
        }
    }

    @Test
    public void testInitialize_WhenDeviceLocaleIsEnglish_ReturnsFalse() {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                mockStatic(ServiceProvider.class)) {
            // Setup
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getActiveLocale()).thenReturn(Locale.ENGLISH);

            // Test
            PropositionTranslationUtility.InitializationResult result =
                    translationUtility.initialize(null);

            // Verify
            assertEquals(
                    PropositionTranslationUtility.InitializationStatus.FAILED_ENGLISH_LOCALE,
                    result.getStatus());
            assertFalse(translationUtility.isTranslationEnabled());
            assertNull(translationUtility.getTargetLanguageCode());
        }
    }

    @Test
    public void testInitialize_WhenDeviceLocaleIsNull_ReturnsFalse() {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                mockStatic(ServiceProvider.class)) {
            // Setup
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getActiveLocale()).thenReturn(null);

            // Test
            PropositionTranslationUtility.InitializationResult result =
                    translationUtility.initialize(null);

            // Verify - when system locale is null, app locale is used; either way translation
            // should not be enabled (either FAILED_NO_LOCALE or e.g. FAILED_ENGLISH_LOCALE)
            assertNotNull(result.getStatus());
            assertFalse(
                    result.getStatus()
                            == PropositionTranslationUtility.InitializationStatus
                                    .SUCCESS_MODEL_CACHED);
            assertFalse(
                    result.getStatus()
                            == PropositionTranslationUtility.InitializationStatus
                                    .SUCCESS_MODEL_DOWNLOADED);
            assertFalse(translationUtility.isTranslationEnabled());
        }
    }

    @Test
    public void testTranslatePropositionItem_WhenTranslationDisabled_ReturnsOriginal()
            throws MessageRequiredFieldMissingException {
        // Setup - translation not initialized
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("content", "Hello World");

        PropositionItem originalItem =
                new PropositionItem("item1", SchemaType.HTML_CONTENT, itemData);

        // Test
        PropositionItem result = translationUtility.translatePropositionItem(originalItem);

        // Verify - should return the same instance
        assertEquals(originalItem, result);
    }

    @Test
    public void testTranslatePropositionItem_WithEmptyItemData_ReturnsOriginal()
            throws MessageRequiredFieldMissingException {
        // Setup
        Map<String, Object> emptyData = new HashMap<>();
        PropositionItem originalItem =
                new PropositionItem("item1", SchemaType.HTML_CONTENT, emptyData);

        // Test
        PropositionItem result = translationUtility.translatePropositionItem(originalItem);

        // Verify
        assertEquals(originalItem, result);
    }

    @Test
    public void testCleanup_ReleasesResources() {
        // Test
        translationUtility.cleanup();

        // Verify
        assertFalse(translationUtility.isTranslationEnabled());
        assertNull(translationUtility.getTargetLanguageCode());
    }

    @Test
    public void testIsTranslationEnabled_InitiallyFalse() {
        // Test & Verify
        assertFalse(translationUtility.isTranslationEnabled());
    }

    @Test
    public void testGetTargetLanguageCode_InitiallyNull() {
        // Test & Verify
        assertNull(translationUtility.getTargetLanguageCode());
    }

    @Test
    public void testTranslatePropositionItem_WithNullPropositionItem_HandlesGracefully() {
        // Test - should not throw exception
        PropositionItem result = translationUtility.translatePropositionItem(null);

        // Verify - returns null when input is null
        assertNull(result);
    }

    // Note: Integration tests with actual ML Kit translation would require:
    // 1. Network connection for model download
    // 2. Google Play Services on device
    // 3. Longer test execution time
    // These should be implemented as instrumented tests (androidTest) rather than unit tests
}

