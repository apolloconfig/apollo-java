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
package com.ctrip.framework.apollo.util;

import com.ctrip.framework.apollo.core.ConfigConsts;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import java.io.File;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigUtilTest {

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ConfigConsts.APOLLO_CLUSTER_KEY);
    System.clearProperty("apollo.connectTimeout");
    System.clearProperty("apollo.readTimeout");
    System.clearProperty("apollo.refreshInterval");
    System.clearProperty("apollo.loadConfigQPS");
    System.clearProperty("apollo.longPollQPS");
    System.clearProperty("apollo.configCacheSize");
    System.clearProperty("apollo.longPollingInitialDelayInMills");
    System.clearProperty("apollo.autoUpdateInjectedSpringProperties");
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR);
    System.clearProperty(PropertiesFactory.APOLLO_PROPERTY_ORDER_ENABLE);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CACHE_KUBERNETES_NAMESPACE);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_KUBERNETES_CACHE_ENABLE);
  }

  @Test
  public void testApolloCluster() throws Exception {
    String someCluster = "someCluster";
    System.setProperty(ConfigConsts.APOLLO_CLUSTER_KEY, someCluster);

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someCluster, configUtil.getCluster());
  }

  @Test
  public void testCustomizeConnectTimeout() throws Exception {
    int someConnectTimeout = 1;
    System.setProperty("apollo.connectTimeout", String.valueOf(someConnectTimeout));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someConnectTimeout, configUtil.getConnectTimeout());
  }

  @Test
  public void testCustomizeInvalidConnectTimeout() throws Exception {
    String someInvalidConnectTimeout = "a";
    System.setProperty("apollo.connectTimeout", someInvalidConnectTimeout);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getConnectTimeout() > 0);
  }

  @Test
  public void testCustomizeReadTimeout() throws Exception {
    int someReadTimeout = 1;
    System.setProperty("apollo.readTimeout", String.valueOf(someReadTimeout));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someReadTimeout, configUtil.getReadTimeout());
  }

  @Test
  public void testCustomizeInvalidReadTimeout() throws Exception {
    String someInvalidReadTimeout = "a";
    System.setProperty("apollo.readTimeout", someInvalidReadTimeout);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getReadTimeout() > 0);
  }

  @Test
  public void testCustomizeRefreshInterval() throws Exception {
    int someRefreshInterval = 1;
    System.setProperty("apollo.refreshInterval", String.valueOf(someRefreshInterval));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someRefreshInterval, configUtil.getRefreshInterval());
  }

  @Test
  public void testCustomizeInvalidRefreshInterval() throws Exception {
    String someInvalidRefreshInterval = "a";
    System.setProperty("apollo.refreshInterval", someInvalidRefreshInterval);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getRefreshInterval() > 0);
  }

  @Test
  public void testCustomizeLoadConfigQPS() throws Exception {
    int someQPS = 1;
    System.setProperty("apollo.loadConfigQPS", String.valueOf(someQPS));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someQPS, configUtil.getLoadConfigQPS());
  }

  @Test
  public void testCustomizeInvalidLoadConfigQPS() throws Exception {
    String someInvalidQPS = "a";
    System.setProperty("apollo.loadConfigQPS", someInvalidQPS);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getLoadConfigQPS() > 0);
  }

  @Test
  public void testCustomizeLongPollQPS() throws Exception {
    int someQPS = 1;
    System.setProperty("apollo.longPollQPS", String.valueOf(someQPS));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someQPS, configUtil.getLongPollQPS());
  }

  @Test
  public void testCustomizeInvalidLongPollQPS() throws Exception {
    String someInvalidQPS = "a";
    System.setProperty("apollo.longPollQPS", someInvalidQPS);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getLongPollQPS() > 0);
  }

  @Test
  public void testCustomizeMaxConfigCacheSize() throws Exception {
    long someCacheSize = 1;
    System.setProperty("apollo.configCacheSize", String.valueOf(someCacheSize));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someCacheSize, configUtil.getMaxConfigCacheSize());
  }

  @Test
  public void testCustomizeInvalidMaxConfigCacheSize() throws Exception {
    String someInvalidCacheSize = "a";
    System.setProperty("apollo.configCacheSize", someInvalidCacheSize);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getMaxConfigCacheSize() > 0);
  }

  @Test
  public void testCustomizeLongPollingInitialDelayInMills() throws Exception {
    long someLongPollingDelayInMills = 1;
    System.setProperty("apollo.longPollingInitialDelayInMills",
        String.valueOf(someLongPollingDelayInMills));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someLongPollingDelayInMills, configUtil.getLongPollingInitialDelayInMills());
  }

  @Test
  public void testCustomizeInvalidLongPollingInitialDelayInMills() throws Exception {
    String someInvalidLongPollingDelayInMills = "a";
    System.setProperty("apollo.longPollingInitialDelayInMills", someInvalidLongPollingDelayInMills);

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.getLongPollingInitialDelayInMills() > 0);
  }

  @Test
  public void testCustomizeAutoUpdateInjectedSpringProperties() throws Exception {
    boolean someAutoUpdateInjectedSpringProperties = false;
    System.setProperty("apollo.autoUpdateInjectedSpringProperties",
        String.valueOf(someAutoUpdateInjectedSpringProperties));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someAutoUpdateInjectedSpringProperties,
        configUtil.isAutoUpdateInjectedSpringPropertiesEnabled());
  }

  @Test
  public void testLocalCacheDirWithSystemProperty() throws Exception {
    String someCacheDir = "someCacheDir";
    String someAppId = "someAppId";

    System.setProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR, someCacheDir);

    ConfigUtil configUtil = spy(new ConfigUtil());

    doReturn(someAppId).when(configUtil).getAppId();

    assertEquals(someCacheDir + File.separator + someAppId, configUtil.getDefaultLocalCacheDir(someAppId));
  }

  @Test
  public void testDefaultLocalCacheDir() throws Exception {
    String someAppId = "someAppId";

    ConfigUtil configUtil = spy(new ConfigUtil());

    doReturn(someAppId).when(configUtil).getAppId();

    doReturn(true).when(configUtil).isOSWindows();

    assertEquals("C:\\opt\\data\\" + someAppId, configUtil.getDefaultLocalCacheDir(someAppId));

    doReturn(false).when(configUtil).isOSWindows();

    assertEquals("/opt/data/" + someAppId, configUtil.getDefaultLocalCacheDir(someAppId));
  }

  @Test
  public void testK8sNamespaceWithSystemProperty() {
    String someK8sNamespace = "someK8sNamespace";

    System.setProperty(ApolloClientSystemConsts.APOLLO_CACHE_KUBERNETES_NAMESPACE, someK8sNamespace);

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someK8sNamespace, configUtil.getK8sNamespace());
  }

  @Test
  public void testK8sNamespaceWithDefault() {
    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(ConfigConsts.KUBERNETES_CACHE_CONFIG_MAP_NAMESPACE_DEFAULT, configUtil.getK8sNamespace());
  }

  @Test
  public void testKubernetesCacheEnabledWithSystemProperty() {
    boolean someKubernetesCacheEnabled = true;

    System.setProperty(ApolloClientSystemConsts.APOLLO_KUBERNETES_CACHE_ENABLE, String.valueOf(someKubernetesCacheEnabled));

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.isPropertyKubernetesCacheEnabled());
  }

  @Test
  public void testCustomizePropertiesOrdered() {
    boolean propertiesOrdered = true;
    System.setProperty(PropertiesFactory.APOLLO_PROPERTY_ORDER_ENABLE,
        String.valueOf(propertiesOrdered));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(propertiesOrdered,
        configUtil.isPropertiesOrderEnabled());
  }

  @Test
  public void testMonitorExternalType() {
    String someMonitorExternalType = "someType";
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE, someMonitorExternalType);

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someMonitorExternalType, configUtil.getMonitorExternalType());
  }

  @Test
  public void testCustomizeMonitorExternalCollectPeriod() {
    int somePeriod = 5;
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD, String.valueOf(somePeriod));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(somePeriod, configUtil.getMonitorExternalExportPeriod());
  }

  @Test
  public void testCustomizeInvalidMonitorExternalCollectPeriod() {
    String someInvalidPeriod = "a";
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD, someInvalidPeriod);

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(10, configUtil.getMonitorExternalExportPeriod()); // Default value
  }

  @Test
  public void testClientMonitorEnabled() {
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_ENABLED, "true");

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.isClientMonitorEnabled());
  }

  @Test
  public void testClientMonitorEnabledDefault() {
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_ENABLED);

    ConfigUtil configUtil = new ConfigUtil();

    assertFalse(configUtil.isClientMonitorEnabled()); // Default value
  }

  @Test
  public void testClientMonitorJmxEnabled() {
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_JMX_ENABLED, "true");

    ConfigUtil configUtil = new ConfigUtil();

    assertTrue(configUtil.isClientMonitorJmxEnabled());
  }

  @Test
  public void testClientMonitorJmxEnabledDefault() {
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_JMX_ENABLED);

    ConfigUtil configUtil = new ConfigUtil();

    assertFalse(configUtil.isClientMonitorJmxEnabled()); // Default value
  }

  @Test
  public void testCustomizeMonitorExceptionQueueSize() {
    int someQueueSize = 10;
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXCEPTION_QUEUE_SIZE, String.valueOf(someQueueSize));

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(someQueueSize, configUtil.getMonitorExceptionQueueSize());
  }

  @Test
  public void testCustomizeInvalidMonitorExceptionQueueSize() {
    String someInvalidQueueSize = "a";
    System.setProperty(ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXCEPTION_QUEUE_SIZE, someInvalidQueueSize);

    ConfigUtil configUtil = new ConfigUtil();

    assertEquals(25, configUtil.getMonitorExceptionQueueSize()); // Default value
  }

  @Test
  public void test() {
    ConfigUtil configUtil = new ConfigUtil();
    assertFalse(configUtil.isPropertyNamesCacheEnabled());

    System.setProperty(ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE, "true");
    configUtil = new ConfigUtil();
    assertTrue(configUtil.isPropertyNamesCacheEnabled());
  }
}
