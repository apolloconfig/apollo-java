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
package com.ctrip.framework.apollo.monitor.internal.event;

import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListenerManager;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class ApolloClientMonitorEventPublisherTest {

  private ApolloClientMonitorEventListenerManager mockCollectorManager;
  private ConfigUtil mockConfigUtil;
  private ApolloClientMonitorEventListener mockCollector;
  private ApolloClientMonitorEvent mockEvent;

  @Before
  public void setUp() {
    mockCollectorManager = mock(ApolloClientMonitorEventListenerManager.class);
    mockConfigUtil = mock(ConfigUtil.class);
    mockCollector = mock(ApolloClientMonitorEventListener.class);
    mockEvent = mock(ApolloClientMonitorEvent.class);

    // 使用 Mockito 来模拟静态方法
    MockInjector.setInstance(ApolloClientMonitorEventListenerManager.class, mockCollectorManager);
    MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);
    ApolloClientMonitorEventPublisher.reset();
  }

  @Test
  public void testPublish_WhenClientMonitorEnabled_CollectorSupportsEvent() {
    when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(true);
    when(mockCollectorManager.getCollectors()).thenReturn(Collections.singletonList(mockCollector));
    when(mockCollector.isSupport(mockEvent)).thenReturn(true);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockCollector).collect(mockEvent);
  }

  @Test
  public void testPublish_WhenClientMonitorEnabled_CollectorDoesNotSupportEvent() {
    when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(true);
    when(mockCollectorManager.getCollectors()).thenReturn(Collections.singletonList(mockCollector));
    when(mockCollector.isSupport(mockEvent)).thenReturn(false);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockCollector, never()).collect(mockEvent);
  }

  @Test
  public void testPublish_WhenClientMonitorDisabled() {
    when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(false);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockCollectorManager, never()).getCollectors();
  }
}
