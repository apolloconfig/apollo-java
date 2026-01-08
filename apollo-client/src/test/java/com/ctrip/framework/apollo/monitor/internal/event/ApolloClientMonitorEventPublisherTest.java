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
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorContext;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ApolloClientMonitorEventPublisherTest {

  private ApolloClientMonitorContext mockCollectorManager;
  private ConfigUtil mockConfigUtil;
  private ApolloClientMonitorEventListener mockListener;
  private ApolloClientMonitorEvent mockEvent;

  @BeforeEach
  public void setUp() {
    mockCollectorManager = mock(ApolloClientMonitorContext.class);
    mockConfigUtil = mock(ConfigUtil.class);
    mockListener = mock(ApolloClientMonitorEventListener.class);
    mockEvent = mock(ApolloClientMonitorEvent.class);

    // 使用 Mockito 来模拟静态方法
    MockInjector.setInstance(ApolloClientMonitorContext.class, mockCollectorManager);
    MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);
    ApolloClientMonitorEventPublisher.reset();
  }

  @AfterEach
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testPublish_WhenClientMonitorEnabled_CollectorSupportsEvent() {
    when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(true);
    when(mockCollectorManager.getApolloClientMonitorEventListeners()).thenReturn(Collections.singletonList(
        mockListener));
    when(mockListener.isSupported(mockEvent)).thenReturn(true);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockListener).collect(mockEvent);
  }

  @Test
  public void testPublish_WhenClientMonitorEnabled_CollectorDoesNotSupportEvent() {
    when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(true);
    when(mockCollectorManager.getApolloClientMonitorEventListeners()).thenReturn(Collections.singletonList(mockListener));
    when(mockListener.isSupported(mockEvent)).thenReturn(false);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockListener, never()).collect(mockEvent);
  }

  @Test
  public void testPublish_WhenClientMonitorDisabled() {
    when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(false);

    ApolloClientMonitorEventPublisher.publish(mockEvent);

    verify(mockCollectorManager, never()).getApolloClientMonitorEventListeners();
  }
}
