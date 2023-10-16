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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class InboundTypeTests {
    private static final String SCHEMA_FEED_ITEM = "https://ns.adobe.com/personalization/message/feed-item";
    private static final String SCHEMA_IAM = "https://ns.adobe.com/personalization/message/in-app";
    private static final String SCHEMA_UNKNOWN = "unknown";

    @Test
    public void test_createFeedInboundType() {
        // test
        InboundType inboundType = InboundType.FEED;
        // verify
        assertEquals(1, inboundType.getValue());
        assertEquals(SCHEMA_FEED_ITEM, inboundType.toString());
    }

    @Test
    public void test_createInAppInboundType() {
        // test
        InboundType inboundType = InboundType.INAPP;
        // verify
        assertEquals(2, inboundType.getValue());
        assertEquals(SCHEMA_IAM, inboundType.toString());
    }

    @Test
    public void test_createUnknownInboundType() {
        // test
        InboundType inboundType = InboundType.UNKNOWN;
        // verify
        assertEquals(0, inboundType.getValue());
        assertEquals(SCHEMA_UNKNOWN, inboundType.toString());
    }

    @Test
    public void test_createFeedInboundTypeFromString() {
        // test
        InboundType inboundType = InboundType.fromString(InboundType.SCHEMA_FEED_ITEM);
        // verify
        assertEquals(1, inboundType.getValue());
        assertEquals(SCHEMA_FEED_ITEM, inboundType.toString());
    }

    @Test
    public void test_createInAppInboundTypeFromString() {
        // test
        InboundType inboundType = InboundType.fromString(InboundType.SCHEMA_IAM);
        // verify
        assertEquals(2, inboundType.getValue());
        assertEquals(SCHEMA_IAM, inboundType.toString());
    }

    @Test
    public void test_createUnknownInboundTypeFromString() {
        // test
        InboundType inboundType = InboundType.fromString("unsupported");
        // verify
        assertEquals(0, inboundType.getValue());
        assertEquals(SCHEMA_UNKNOWN, inboundType.toString());
    }
}