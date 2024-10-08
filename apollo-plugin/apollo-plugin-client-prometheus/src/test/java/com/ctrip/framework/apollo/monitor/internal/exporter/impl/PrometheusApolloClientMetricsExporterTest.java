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

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PrometheusApolloClientMetricsExporterTest {

    private PrometheusApolloClientMetricsExporter exporter;

    @Before
    public void setUp() {
        exporter = new PrometheusApolloClientMetricsExporter();
        exporter.doInit();
    }

    @Test
    public void testIsSupport() {
        assertTrue(exporter.isSupport("prometheus"));
        assertFalse(exporter.isSupport("other"));
    }

    @Test
    public void testRegisterOrUpdateCounterSample() {
        String name = "test_counter";
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1", "value1");

        exporter.registerOrUpdateCounterSample(name, tags, 1.0);
        
        Counter counter = (Counter) exporter.map.get(name);
        
        assertNotNull(counter);
        assertEquals(1.0, counter.labels("value1").get(), 0.001);
    }

    @Test
    public void testRegisterOrUpdateGaugeSample() {
        String name = "test_gauge";
        Map<String, String> tags = new HashMap<>();
        tags.put("tag2", "value2");

        exporter.registerOrUpdateGaugeSample(name, tags, 3.0);
        
        Gauge gauge = (Gauge) exporter.map.get(name);
        
        assertNotNull(gauge);
        assertEquals(3.0, gauge.labels("value2").get(), 0.001);
    }

    @Test
    public void testResponse() {
        String response = exporter.response();
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }
}
