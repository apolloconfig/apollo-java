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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class NullClientBootstrapArgsMonitorApiTest {

  private NullClientBootstrapArgsMonitorApi bootstrapArgsMonitorApi;

  @Before
  public void setUp() {
    bootstrapArgsMonitorApi = new NullClientBootstrapArgsMonitorApi();
  }

  @Test
  public void testGetStartupParams() {
    assertEquals("", bootstrapArgsMonitorApi.getStartupParams("testKey"));
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
  public void testGetBootstrapEnabled() {
    assertNull(bootstrapArgsMonitorApi.getBootstrapEnabled());
  }

  @Test
  public void testGetBootstrapNamespaces() {
    assertEquals("", bootstrapArgsMonitorApi.getBootstrapNamespaces());
  }

  @Test
  public void testGetBootstrapEagerLoadEnabled() {
    assertNull(bootstrapArgsMonitorApi.getBootstrapEagerLoadEnabled());
  }

  @Test
  public void testGetOverrideSystemProperties() {
    assertNull(bootstrapArgsMonitorApi.getOverrideSystemProperties());
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
  public void testGetClientMonitorEnabled() {
    assertNull(bootstrapArgsMonitorApi.getClientMonitorEnabled());
  }

  @Test
  public void testGetClientMonitorJmxEnabled() {
    assertNull(bootstrapArgsMonitorApi.getClientMonitorJmxEnabled());
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
  public void testGetPropertyNamesCacheEnable() {
    assertNull(bootstrapArgsMonitorApi.getPropertyNamesCacheEnable());
  }

  @Test
  public void testGetPropertyOrderEnable() {
    assertNull(bootstrapArgsMonitorApi.getPropertyOrderEnable());
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
    Map<String, String> bootstrapArgs = bootstrapArgsMonitorApi.getBootstrapArgs();

    assertNotNull(bootstrapArgs);
    assertTrue(bootstrapArgs.isEmpty());
  }
}
