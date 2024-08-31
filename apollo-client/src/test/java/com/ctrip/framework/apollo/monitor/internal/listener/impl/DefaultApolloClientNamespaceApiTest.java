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

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.APOLLO_CLIENT_NAMESPACE_NOT_FOUND;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.APOLLO_CLIENT_NAMESPACE_TIMEOUT;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.APOLLO_CLIENT_NAMESPACE_USAGE;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.NAMESPACE;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import org.mockito.internal.util.collections.Sets;

public class DefaultApolloClientNamespaceApiTest {

  private DefaultApolloClientNamespaceApi api;
  private Map<String, Config> configs;
  private Map<String, ConfigFile> configFiles;

  @Before
  public void setUp() {
    configs = new HashMap<>();
    configFiles = new HashMap<>();
    api = new DefaultApolloClientNamespaceApi(configs, configFiles);
  }

  @Test
  public void testCollectNamespaceNotFound() {
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getAttachmentValue(NAMESPACE)).thenReturn("testNamespace");
    when(event.getName()).thenReturn(APOLLO_CLIENT_NAMESPACE_NOT_FOUND);

    api.collect0(event);

    assertEquals(1, api.getNotFoundNamespaces().size());
    assertTrue(api.getNotFoundNamespaces().contains("testNamespace"));
  }

  @Test
  public void testCollectNamespaceTimeout() {
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getAttachmentValue(NAMESPACE)).thenReturn("testNamespace");
    when(event.getName()).thenReturn(APOLLO_CLIENT_NAMESPACE_TIMEOUT);

    api.collect0(event);

    assertEquals(1, api.getTimeoutNamespaces().size());
    assertTrue(api.getTimeoutNamespaces().contains("testNamespace"));
  }

  @Test
  public void testCollectNamespaceUsage() {
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getAttachmentValue(NAMESPACE)).thenReturn("testNamespace");
    when(event.getName()).thenReturn(APOLLO_CLIENT_NAMESPACE_USAGE);

    api.collect0(event);

    assertEquals(1, api.getNamespaceMetrics().get("testNamespace").getUsageCount());
  }

  @Test
  public void testGetNamespacePropertySize() {
    Config mockConfig = mock(Config.class);
    when(mockConfig.getPropertyNames()).thenReturn(Sets.newSet("key1", "key2"));
    configs.put("testNamespace", mockConfig);
    Integer testNamespace = api.getNamespacePropertySize("testNamespace");
    assertEquals(2, testNamespace.intValue());
  }

  @Test
  public void testGetConfigFileNamespaces() {
    ConfigFile mockConfigFile = mock(ConfigFile.class);
    configFiles.put("testNamespace", mockConfigFile);
    List<String> configFileNum = api.getConfigFileNamespaces();
    assertEquals(1, configFileNum.size());
  }
}