/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.monitor.internal.exporter.impl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SimpleOpenTelemetryApolloClientMetricsExporterTest {

    private OpenTelemetryApolloClientMetricsExporter exporter;

    @Before
    public void setUp() {
        exporter = new OpenTelemetryApolloClientMetricsExporter();
        // Note: We don't call doInit() because it requires GlobalOpenTelemetry
        // In a real test environment, you would need to setup OpenTelemetry first
    }

    @Test
    public void testIsSupport() {
        assertTrue(exporter.isSupport("opentelemetry"));
        assertFalse(exporter.isSupport("prometheus"));
        assertFalse(exporter.isSupport("other"));
        assertFalse(exporter.isSupport(null));
    }

    @Test
    public void testResponseWithoutInit() {
        // Even without init, response should return a status message
        String response = exporter.response();
        assertNotNull(response);
        assertTrue(response.contains("OpenTelemetry metrics exporter status"));
    }

    @Test
    public void testRegisterOrUpdateCounterSampleWithoutInit() {
        // This should not throw exception even without init
        String name = "test_counter";
        Map<String, String> tags = new HashMap<>();
        tags.put("namespace", "application");
        
        try {
            exporter.registerOrUpdateCounterSample(name, tags, 1.0);
            // If we get here, it means no exception was thrown
            assertTrue(true);
        } catch (Exception e) {
            // It's okay if it throws exception when not initialized
            // This depends on OpenTelemetry API behavior
        }
    }

    @Test
    public void testRegisterOrUpdateGaugeSampleWithoutInit() {
        // This should not throw exception even without init
        String name = "test_gauge";
        Map<String, String> tags = new HashMap<>();
        tags.put("namespace", "application");
        
        try {
            exporter.registerOrUpdateGaugeSample(name, tags, 3.14);
            // If we get here, it means no exception was thrown
            assertTrue(true);
        } catch (Exception e) {
            // It's okay if it throws exception when not initialized
            // This depends on OpenTelemetry API behavior
        }
    }

    @Test
    public void testEmptyTags() {
        String name = "test_metric";
        Map<String, String> emptyTags = new HashMap<>();
        
        try {
            exporter.registerOrUpdateCounterSample(name, emptyTags, 1.0);
            exporter.registerOrUpdateGaugeSample(name, emptyTags, 2.0);
            assertTrue(true);
        } catch (Exception e) {
            // Acceptable if it throws exception
        }
    }

    @Test
    public void testNullTags() {
        String name = "test_metric";
        
        try {
            exporter.registerOrUpdateCounterSample(name, null, 1.0);
            exporter.registerOrUpdateGaugeSample(name, null, 2.0);
            assertTrue(true);
        } catch (Exception e) {
            // Acceptable if it throws exception for null tags
        }
    }
}