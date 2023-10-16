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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.messaging.internal.MessagingTestUtils;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionTests {

    Map<String, Object> characteristics = new HashMap<String, Object>() {{
        put("eventToken", "eventToken");
    }};
    Map<String, Object> activity = new HashMap<String, Object>() {{
        put("id", "activityId");
    }};
    Map<String, Object> scopeDetails = new HashMap<String, Object>() {{
        put("decisionProvider", "AJO");
        put("correlationID", "correlationID");
        put("characteristics", characteristics);
        put("activity", activity);
    }};

    Map<String, Object> propositionItemMap = new HashMap<>();
    Map<String, Object> eventDataMap = new HashMap<>();
    List<PropositionItem> propositionItems = new ArrayList<>();
    List<Map<String, Object>> propositionItemMaps = new ArrayList<>();

    @Before
    public void setup() throws JSONException {
        propositionItemMap = MessagingTestUtils.getMapFromFile("proposition_item.json");
        PropositionItem propositionItem = PropositionItem.fromEventData(propositionItemMap);
        propositionItems.add(propositionItem);
        propositionItemMaps.add(propositionItemMap);
        eventDataMap.put("id", "uniqueId");
        eventDataMap.put("scope", "mobileapp://mockScope");
        eventDataMap.put("scopeDetails", scopeDetails);
        eventDataMap.put("items", propositionItemMaps);
    }

    @Test
    public void test_propositionConstructor() {
        // test
        Proposition proposition = new Proposition("uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        assertEquals(propositionItems, proposition.getItems());
    }

    @Test
    public void test_createProposition_fromEventData() {
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        // need to verify proposition items individually as proposition soft references will be different as the proposition is created from a map
        for (PropositionItem item : proposition.getItems()) {
            assertEquals(propositionItems.get(0).getUniqueId(), item.getUniqueId());
            assertEquals(propositionItems.get(0).getContent(), item.getContent());
            assertEquals(propositionItems.get(0).getSchema(), item.getSchema());
            assertNotNull(item.getProposition());
        }
    }

    @Test
    public void test_createEventData_fromProposition() throws DataReaderException, JSONException {
        // test
        Proposition proposition = new Proposition("uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        Map<String, Object> propositionMap = proposition.toEventData();
        // verify
        assertNotNull(propositionMap);
        List<Map<String, Object>> itemList = DataReader.optTypedListOfMap(Object.class, propositionMap, "items", null);
        PropositionItem propositionItem = propositionItems.get(0);
        for (Map<String, Object> item : itemList) {
            Map<String, Object> data = DataReader.getTypedMap(Object.class, item, "data");
            String content = DataReader.getString(data, "content");
            assertEquals(propositionItem.getUniqueId(), item.get("id"));
            assertEquals(propositionItem.getSchema(), item.get("schema"));
            assertEquals("{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"consequenceId\",\"detail\":{\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\",\"data\":{\"expiryDate\":1717688797,\"publishedDate\":1717688797,\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp/feeds/testFeed\",\"feedName\":\"testFeed\",\"campaignName\":\"testCampaign\"},\"content\":{\"actionUrl\":\"actionUrl\",\"actionTitle\":\"actionTitle\",\"title\":\"title\",\"body\":\"body\",\"imageUrl\":\"imageUrl\"}},\"id\":\"uniqueDetailId\"}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}", content);
        }
    }
}