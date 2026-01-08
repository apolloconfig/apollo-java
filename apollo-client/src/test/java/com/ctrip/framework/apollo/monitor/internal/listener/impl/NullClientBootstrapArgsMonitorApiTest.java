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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NullClientBootstrapArgsMonitorApiTest {

  private NullClientBootstrapArgsMonitorApi bootstrapArgsMonitorApi;

  @BeforeEach
  public void setUp() {
    bootstrapArgsMonitorApi = new NullClientBootstrapArgsMonitorApi();
  }

  @Test
  public void testGetStartupParams() {
      assertNull(bootstrapArgsMonitorApi.getStartupArg("testKey"));
  }

  @Test
  public void testGetConfigServiceUrl() {
    assertEquals("", bootstrapArgsMonitorApi.getConfigServiceUrl());
  }

  @Test
  public void testGetAccessKeySecret() {
    assertEquals("", bootstrapArgsMonitorApi.getAccessKeySecret());
  }

  @Test
  public void testGetAutoUpdateInjectedSpringProperties() {
    assertFalse(bootstrapArgsMonitorApi.getAutoUpdateInjectedSpringProperties());
  }

  @Test
  public void testIsBootstrapEnabled() {
    assertFalse(bootstrapArgsMonitorApi.isBootstrapEnabled());
  }

  @Test
  public void testGetBootstrapNamespaces() {
    assertEquals("", bootstrapArgsMonitorApi.getBootstrapNamespaces());
  }

  @Test
  public void testIsBootstrapEagerLoadEnabled() {
    assertFalse(bootstrapArgsMonitorApi.isBootstrapEagerLoadEnabled());
  }

  @Test
  public void testIsOverrideSystemProperties() {
    assertFalse(bootstrapArgsMonitorApi.isOverrideSystemProperties());
  }

  @Test
  public void testGetCacheDir() {
    assertEquals("", bootstrapArgsMonitorApi.getCacheDir());
  }

  @Test
  public void testGetCluster() {
    assertEquals("", bootstrapArgsMonitorApi.getCluster());
  }

  @Test
  public void testGetConfigService() {
    assertEquals("", bootstrapArgsMonitorApi.getConfigService());
  }

  @Test
  public void testIsClientMonitorEnabled() {
    assertFalse(bootstrapArgsMonitorApi.isClientMonitorEnabled());
  }

  @Test
  public void testIsClientMonitorJmxEnabled() {
    assertFalse(bootstrapArgsMonitorApi.isClientMonitorJmxEnabled());
  }

  @Test
  public void testGetClientMonitorExternalForm() {
    assertEquals("", bootstrapArgsMonitorApi.getClientMonitorExternalForm());
  }

  @Test
  public void testGetClientMonitorExternalExportPeriod() {
    assertEquals(0, bootstrapArgsMonitorApi.getClientMonitorExternalExportPeriod());
  }

  @Test
  public void testGetClientMonitorExceptionSaveSize() {
    assertEquals(0, bootstrapArgsMonitorApi.getClientMonitorExceptionSaveSize());
  }

  @Test
  public void testGetApolloMeta() {
    assertEquals("", bootstrapArgsMonitorApi.getApolloMeta());
  }

  @Test
  public void testGetMetaLatestFreshTime() {
    assertEquals("", bootstrapArgsMonitorApi.getMetaLatestFreshTime());
  }

  @Test
  public void testIsPropertyNamesCacheEnable() {
    assertFalse(bootstrapArgsMonitorApi.isPropertyNamesCacheEnable());
  }

  @Test
  public void testIsPropertyOrderEnable() {
    assertFalse(bootstrapArgsMonitorApi.isPropertyOrderEnable());
  }

  @Test
  public void testGetVersion() {
    assertEquals("", bootstrapArgsMonitorApi.getVersion());
  }

  @Test
  public void testGetEnv() {
    assertEquals("", bootstrapArgsMonitorApi.getEnv());
  }

  @Test
  public void testGetAppId() {
    assertEquals("", bootstrapArgsMonitorApi.getAppId());
  }

  @Test
  public void testGetBootstrapArgs() {
    Map<String, Object> bootstrapArgs = bootstrapArgsMonitorApi.getBootstrapArgs();

    assertNotNull(bootstrapArgs);
    assertTrue(bootstrapArgs.isEmpty());
  }
}
