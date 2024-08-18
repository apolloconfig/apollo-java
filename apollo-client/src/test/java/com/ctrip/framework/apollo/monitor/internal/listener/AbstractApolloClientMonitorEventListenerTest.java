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
package com.ctrip.framework.apollo.monitor.internal.listener;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.SampleModel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractApolloClientMonitorEventListenerTest {

    private class TestMonitorEventListener extends AbstractApolloClientMonitorEventListener {
        public TestMonitorEventListener(String tag) {
            super(tag);
        }

        @Override
        protected void collect0(ApolloClientMonitorEvent event) {
            // 简单的收集逻辑
        }

        @Override
        protected void export0() {
            // 模拟导出逻辑
        }
    }

    private TestMonitorEventListener listener;
    private ApolloClientMonitorEvent event;

    @Before
    public void setUp() {
        listener = new TestMonitorEventListener("testTag");
        event = mock(ApolloClientMonitorEvent.class);
        when(event.getTag()).thenReturn("testTag");
    }

    @Test
    public void testCollect() {
        listener.collect(event);
        assertTrue(listener.isMetricsSampleUpdated());
    }

    @Test
    public void testIsSupport() {
        assertTrue(listener.isSupport(event));
        when(event.getTag()).thenReturn("otherTag");
        assertFalse(listener.isSupport(event));
    }

    @Test
    public void testExport() {
        listener.collect(event);
        List<SampleModel> samples = listener.export();
        assertNotNull(samples);
        assertTrue(samples.isEmpty()); // 应为空，因为尚未添加样本
    }

    @Test
    public void testCreateOrUpdateGaugeSample() {
        String mapKey = "gauge1";
        String metricsName = "testGauge";
        Map<String, String> tags = new HashMap<>();
        tags.put("key", "value");

        listener.createOrUpdateGaugeSample(mapKey, metricsName, tags, 42.0);

        List<SampleModel> samples = listener.export();
        assertEquals(1, samples.size());
        assertTrue(samples.get(0) instanceof GaugeModel);
        assertEquals(42.0, ((GaugeModel) samples.get(0)).getValue(), 0.01);
    }

    @Test
    public void testCreateOrUpdateCounterSample() {
        String mapKey = "counter1";
        String metricsName = "testCounter";
        Map<String, String> tags = new HashMap<>();
        tags.put("key", "value");

        listener.createOrUpdateCounterSample(mapKey, metricsName, tags, 5.0);

        List<SampleModel> samples = listener.export();
        assertEquals(1, samples.size());
        assertTrue(samples.get(0) instanceof CounterModel);
        assertEquals(5.0, ((CounterModel) samples.get(0)).getValue(), 0.01);
    }
}