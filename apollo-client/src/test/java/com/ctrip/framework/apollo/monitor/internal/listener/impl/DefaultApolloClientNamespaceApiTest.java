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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEventFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultApolloClientNamespaceApiTest {

  @Mock
  private ConfigManager configManager;

  @Mock
  private Config config;

  @InjectMocks
  private DefaultApolloClientNamespaceApi namespaceApi;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(configManager.getConfig(anyString())).thenReturn(config);
  }

  @Test
  public void testCollectNamespaceNotFound() {
    ApolloClientMonitorEvent event = ApolloClientMonitorEventFactory
        .getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_NOT_FOUND)
        .putAttachment(NAMESPACE, "testNamespace");

    namespaceApi.collect0(event);

    assertTrue(namespaceApi.getNotFoundNamespaces().contains("testNamespace"));
  }

  @Test
  public void testCollectNamespaceTimeout() {
    ApolloClientMonitorEvent event = ApolloClientMonitorEventFactory
        .getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_TIMEOUT)
        .putAttachment(NAMESPACE, "testNamespace");

    namespaceApi.collect0(event);

    assertTrue(namespaceApi.getTimeoutNamespaces().contains("testNamespace"));
  }

  @Test
  public void testCollectNormalNamespace() {
    ApolloClientMonitorEvent event = ApolloClientMonitorEventFactory
        .getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_USAGE)
        .putAttachment(NAMESPACE, "testNamespace");

    namespaceApi.collect0(event);

    // Verify that the usage count has been incremented
    assertEquals(1, namespaceApi.getNamespaceMetrics().get("testNamespace").getUsageCount());
  }

  @Test
  public void testGetNamespacePropertySize() {
    when(config.getPropertyNames()).thenReturn(Collections.singleton("property1"));

    Integer propertySize = namespaceApi.getNamespacePropertySize("testNamespace");

    assertEquals(Integer.valueOf(1), propertySize);
  }

  @Test
  public void testExportMetrics() {
    // Set up some initial state
    ApolloClientMonitorEvent event = ApolloClientMonitorEventFactory
        .getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_USAGE)
        .putAttachment(NAMESPACE, "testNamespace");
    namespaceApi.collect0(event);

    // Call the export method
    namespaceApi.export0();

    // Verify interactions with the configManager
    verify(configManager).getConfig("testNamespace");
  }
}