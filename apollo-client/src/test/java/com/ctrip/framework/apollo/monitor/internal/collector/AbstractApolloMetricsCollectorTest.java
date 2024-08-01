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
package com.ctrip.framework.apollo.monitor.internal.collector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsModel;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
public class AbstractApolloMetricsCollectorTest {

  private AbstractMetricsCollector metricsCollector;

  @Before
  public void setUp() {
    metricsCollector = new AbstractMetricsCollector("mock","tag1", "tag2") {
      @Override
      public String name() {
        return "MockMetricsCollector";
      }

      @Override
      public void collect0(MetricsEvent event) {
        // 模拟实现
      }

      @Override
      public void export0() {
        // 模拟实现
      }
    };
  }

  @Test
  public void testConstructorInitialization() {
    assertNotNull(metricsCollector);
  }

  @Test
  public void testIsSupport() {
    MetricsEvent event = Mockito.mock(MetricsEvent.class);

    when(event.getTag()).thenReturn("tag1");
    assertTrue(metricsCollector.isSupport(event));

    when(event.getTag()).thenReturn("tag3");
    assertFalse(metricsCollector.isSupport(event));
  }

  @Test
  public void testCollect() {
    MetricsEvent event = Mockito.mock(MetricsEvent.class);
    metricsCollector.collect(event);
    assertTrue(metricsCollector.isSamplesUpdated());
  }

  @Test
  public void testIsSamplesUpdated() {
    MetricsEvent event = Mockito.mock(MetricsEvent.class);
    metricsCollector.collect(event);
    assertTrue(metricsCollector.isSamplesUpdated());
    assertFalse(metricsCollector.isSamplesUpdated());
  }

  @Test
  public void testExport() {
    List<MetricsModel> samples = metricsCollector.export();
    assertNotNull(samples);
  }
}
