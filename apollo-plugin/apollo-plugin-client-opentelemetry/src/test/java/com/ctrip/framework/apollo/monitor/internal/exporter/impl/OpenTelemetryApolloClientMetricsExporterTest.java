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
import static org.mockito.Mockito.*;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

public class OpenTelemetryApolloClientMetricsExporterTest {

    private OpenTelemetryApolloClientMetricsExporter exporter;
    private MockedStatic<GlobalOpenTelemetry> globalOpenTelemetryMock;
    private OpenTelemetry openTelemetry;
    private Meter meter;

    @Before
    public void setUp() {
        // Mock GlobalOpenTelemetry
        globalOpenTelemetryMock = mockStatic(GlobalOpenTelemetry.class);
        openTelemetry = mock(OpenTelemetry.class);
        meter = mock(Meter.class);
        
        when(GlobalOpenTelemetry.get()).thenReturn(openTelemetry);
        when(openTelemetry.getMeter("apollo-client")).thenReturn(meter);
        
        exporter = new OpenTelemetryApolloClientMetricsExporter();
        exporter.doInit();
    }

    @After
    public void tearDown() {
        if (globalOpenTelemetryMock != null) {
            globalOpenTelemetryMock.close();
        }
    }

    @Test
    public void testIsSupport() {
        assertTrue(exporter.isSupport("opentelemetry"));
        assertFalse(exporter.isSupport("prometheus"));
        assertFalse(exporter.isSupport("other"));
    }

    @Test
    public void testDoInit() {
        // Verify that meter was obtained from GlobalOpenTelemetry
        globalOpenTelemetryMock.verify(() -> GlobalOpenTelemetry.get());
        verify(openTelemetry).getMeter("apollo-client");
    }

    @Test
    public void testRegisterOrUpdateCounterSample() {
        String name = "test_counter";
        Map<String, String> tags = new HashMap<>();
        tags.put("namespace", "application");
        tags.put("cluster", "default");

        // Mock counter builder
        io.opentelemetry.api.metrics.LongCounterBuilder counterBuilder = mock(io.opentelemetry.api.metrics.LongCounterBuilder.class);
        io.opentelemetry.api.metrics.LongCounter counter = mock(io.opentelemetry.api.metrics.LongCounter.class);
        
        when(meter.counterBuilder(name)).thenReturn(counterBuilder);
        when(counterBuilder.setDescription("Apollo counter metrics")).thenReturn(counterBuilder);
        when(counterBuilder.setUnit("1")).thenReturn(counterBuilder);
        when(counterBuilder.build()).thenReturn(counter);

        // This will create the counter on first call
        exporter.registerOrUpdateCounterSample(name, tags, 1.0);
        
        // Verify counter was created
        verify(meter).counterBuilder(name);
        verify(counterBuilder).build();
    }

    @Test
    public void testRegisterOrUpdateGaugeSample() {
        String name = "test_gauge";
        Map<String, String> tags = new HashMap<>();
        tags.put("namespace", "application");
        tags.put("cluster", "default");

        // Mock gauge builder
        io.opentelemetry.api.metrics.DoubleGaugeBuilder gaugeBuilder = mock(io.opentelemetry.api.metrics.DoubleGaugeBuilder.class);
        io.opentelemetry.api.metrics.ObservableDoubleGauge gauge = mock(io.opentelemetry.api.metrics.ObservableDoubleGauge.class);
        
        when(meter.gaugeBuilder(name)).thenReturn(gaugeBuilder);
        when(gaugeBuilder.setDescription("Apollo gauge metrics")).thenReturn(gaugeBuilder);
        when(gaugeBuilder.setUnit("1")).thenReturn(gaugeBuilder);
        when(gaugeBuilder.buildWithCallback(any())).thenReturn(gauge);

        // This will create the gauge on first call
        exporter.registerOrUpdateGaugeSample(name, tags, 3.14);
        
        // Verify gauge was created
        verify(meter).gaugeBuilder(name);
        verify(gaugeBuilder).buildWithCallback(any());
    }

    @Test
    public void testResponse() {
        String response = exporter.response();
        assertNotNull(response);
        assertTrue(response.contains("OpenTelemetry metrics exporter status"));
        assertTrue(response.contains("Counters: 0"));
        assertTrue(response.contains("Gauges: 0"));
        assertTrue(response.contains("Meter: apollo-client"));
    }

    @Test
    public void testResponseWithMetrics() {
        // Register some metrics first
        Map<String, String> tags = new HashMap<>();
        tags.put("test", "value");
        
        // Mock counter creation
        io.opentelemetry.api.metrics.LongCounterBuilder counterBuilder = mock(io.opentelemetry.api.metrics.LongCounterBuilder.class);
        io.opentelemetry.api.metrics.LongCounter counter = mock(io.opentelemetry.api.metrics.LongCounter.class);
        when(meter.counterBuilder("test_counter")).thenReturn(counterBuilder);
        when(counterBuilder.setDescription(anyString())).thenReturn(counterBuilder);
        when(counterBuilder.setUnit(anyString())).thenReturn(counterBuilder);
        when(counterBuilder.build()).thenReturn(counter);
        
        // Mock gauge creation
        io.opentelemetry.api.metrics.DoubleGaugeBuilder gaugeBuilder = mock(io.opentelemetry.api.metrics.DoubleGaugeBuilder.class);
        io.opentelemetry.api.metrics.ObservableDoubleGauge gauge = mock(io.opentelemetry.api.metrics.ObservableDoubleGauge.class);
        when(meter.gaugeBuilder("test_gauge")).thenReturn(gaugeBuilder);
        when(gaugeBuilder.setDescription(anyString())).thenReturn(gaugeBuilder);
        when(gaugeBuilder.setUnit(anyString())).thenReturn(gaugeBuilder);
        when(gaugeBuilder.buildWithCallback(any())).thenReturn(gauge);

        exporter.registerOrUpdateCounterSample("test_counter", tags, 1.0);
        exporter.registerOrUpdateGaugeSample("test_gauge", tags, 2.0);
        
        String response = exporter.response();
        assertTrue(response.contains("Counters: 1"));
        assertTrue(response.contains("Gauges: 1"));
    }
}