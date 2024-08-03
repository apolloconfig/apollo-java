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
package com.ctrip.framework.apollo.monitor.internal.exporter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.monitor.internal.enums.MeterEnums;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.SampleModel;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AbstractApolloClientMetricsExporterTest {

    private class TestMetricsExporter extends AbstractApolloClientMetricsExporter {
        @Override
        protected void doInit() {
        }

        public List<ApolloClientMetricsEventListener> getCollectors() {
            return collectors;
        }

      @Override
      public boolean isSupport(String form) {
        return false;
      }

      @Override
      public void registerOrUpdateCounterSample(String name, Map<String, String> tag,
          double incrValue) {

      }

      @Override
      public void registerOrUpdateGaugeSample(String name, Map<String, String> tag, double value) {

      }

      @Override
      public String response() {
        return "";
      }
    }

    private TestMetricsExporter exporter;
    private ApolloClientMetricsEventListener mockListener;

    @Before
    public void setUp() {
        exporter = new TestMetricsExporter();
        mockListener = mock(ApolloClientMetricsEventListener.class);
    }

    @Test
    public void testInit() {
        List<ApolloClientMetricsEventListener> collectors = new ArrayList<>();
        collectors.add(mockListener);
        long collectPeriod = 10L;

        exporter.init(collectors, collectPeriod);

        assertEquals(collectors, exporter.getCollectors());
    }

    @Test
    public void testUpdateMetricsData() {
        List<SampleModel> samples = new ArrayList<>();
        GaugeModel gauge = mock(GaugeModel.class);
        when(gauge.getType()).thenReturn(MeterEnums.GAUGE);
        when(gauge.getName()).thenReturn("testGauge");
        when(gauge.getValue()).thenReturn(10.0);
        samples.add(gauge);

        when(mockListener.isMetricsSampleUpdated()).thenReturn(true);
        when(mockListener.export()).thenReturn(samples);

        exporter.init(Collections.singletonList(mockListener), 10L);
        exporter.updateMetricsData();

        verify(mockListener).export();
        verify(gauge).getValue();
    }

    @Test
    public void testRegisterSampleGauge() {
        GaugeModel gaugeModel = (GaugeModel) GaugeModel.create("testGauge", 5.0).putTag("key", "value");

        exporter.registerSample(gaugeModel);
    }

    @Test
    public void testRegisterSampleCounter() {
        CounterModel counterModel = (CounterModel) CounterModel.create("testCounter", 5.0).putTag("key", "value");
        exporter.registerSample(counterModel);
    }
    
}