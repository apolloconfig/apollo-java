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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.monitor.internal.collector.MetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsModel;
import com.ctrip.framework.apollo.monitor.internal.util.MeterType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractApolloMetricsExporterTest {

  @Mock
  private MetricsCollector mockCollector;

  @InjectMocks
  private final AbstractMetricsExporter reporter = new AbstractMetricsExporter() {
    @Override
    protected void doInit() {
      // Do nothing for test purposes
    }

    @Override
    public void registerGaugeSample(GaugeModel<?> sample) {
      // Mock implementation for test purposes
    }

    @Override
    public String response() {
      return "test";
    }

    @Override
    public boolean isSupport(String form) {
      return "mock".equals(form);
    }

    @Override
    public void registerCounterSample(CounterModel sample) {
      // Mock implementation for test purposes
    }
  };



  @Test
  public void testInit() {
    List<MetricsCollector> collectors = Collections.singletonList(mockCollector);
    long collectPeriod = 10L;

    reporter.init(collectors, collectPeriod);

    assertNotNull(reporter.m_executorService);
  }
  @Test
  public void testIsSupport(){
     assertTrue(reporter.isSupport("mock"));
     assertFalse(reporter.isSupport("mock1"));
  }

  @Test
  public void testUpdateMetricsData() {
    MetricsModel mockSample = mock(MetricsModel.class);
    when(mockSample.getType()).thenReturn(MeterType.GAUGE);
    when(mockCollector.isSamplesUpdated()).thenReturn(true);
    when(mockCollector.export()).thenReturn(Collections.singletonList(mockSample));

    reporter.init(Collections.singletonList(mockCollector), 10L);
    reporter.updateMetricsData();

    verify(mockCollector, times(1)).isSamplesUpdated();
    verify(mockCollector, times(1)).export();
    verify(mockSample, times(1)).getType();
  }

  @Test
  public void testGetTags() {
    MetricsModel sample = mock(MetricsModel.class);
    Map<String, String> tags = new HashMap<>();
    tags.put("key1", "value1");
    tags.put("key2", "value2");

    when(sample.getTags()).thenReturn(tags);

    String[][] result = reporter.getTags(sample);

    assertArrayEquals(new String[]{"key1", "key2"}, result[0]);
    assertArrayEquals(new String[]{"value1", "value2"}, result[1]);
  }
}
