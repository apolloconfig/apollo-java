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

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultApolloClientBootstrapArgsApiTest {

  private ConfigUtil configUtil;
  private DefaultApolloClientBootstrapArgsApi api;

  @BeforeEach
  public void setUp() {
    configUtil = mock(ConfigUtil.class);
    when(configUtil.getAccessKeySecret()).thenReturn("secret");
    when(configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()).thenReturn(true);
    when(configUtil.isOverrideSystemProperties()).thenReturn(false);
    when(configUtil.getDefaultLocalCacheDir()).thenReturn("/cache");
    when(configUtil.getCluster()).thenReturn("default");
    when(configUtil.getAppId()).thenReturn("myApp");
    when(configUtil.getApolloEnv()).thenReturn(Env.DEV);
    when(configUtil.getMetaServerDomainName()).thenReturn("http://meta.server");

    api = new DefaultApolloClientBootstrapArgsApi(configUtil);
  }

  @Test
  public void testGetAccessKeySecret() {
    assertEquals("secret", api.getAccessKeySecret());
  }

  @Test
  public void testGetAutoUpdateInjectedSpringProperties() {
    assertTrue(api.getAutoUpdateInjectedSpringProperties());
  }

  @Test
  public void testGetCacheDir() {
    assertEquals("/cache", api.getCacheDir());
  }

  @Test
  public void testCollect0() {
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getName()).thenReturn(APOLLO_ACCESS_KEY_SECRET);
    when(event.getAttachmentValue(APOLLO_ACCESS_KEY_SECRET)).thenReturn("newSecret");

    api.collect0(event);

    assertEquals("newSecret", api.getAccessKeySecret());
  }

  @Test
  public void testUnhandledEvent() {
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getName()).thenReturn("unknownEvent");
    api.collect0(event);
  }

  @Test
  public void testGetBootstrapArgs() {
    Map<String, Object> bootstrapArgs = api.getBootstrapArgs();
    assertNotNull(bootstrapArgs);
    assertTrue(bootstrapArgs.containsKey(APOLLO_ACCESS_KEY_SECRET));
  }
}