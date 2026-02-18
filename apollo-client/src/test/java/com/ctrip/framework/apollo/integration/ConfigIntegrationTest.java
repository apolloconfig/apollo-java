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
package com.ctrip.framework.apollo.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.BaseIntegrationTest;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.MockedConfigService;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.ctrip.framework.apollo.util.OrderedProperties;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigIntegrationTest extends BaseIntegrationTest {

  private final String someReleaseKey = "1";

  private final String someOtherNamespace = "someOtherNamespace";
  private static final String FALLBACK_ANOTHER_APP_ID = "100004459";
  private static final String MULTI_APP_ANOTHER_APP_ID = "200000001";
  private static final String PUBLIC_NAMESPACE = "TEST1.apollo";
  private static final String DEFAULT_VALUE = "undefined";
  private static final String MULTI_APP_KEY = "someKey";

  @Test
  public void testGetConfigWithNoLocalFileButWithRemoteConfig() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someNonExistedKey = "someNonExistedKey";
    String someDefaultValue = "someDefaultValue";
    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.of(someKey, someValue));
    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    assertEquals(someValue, config.getProperty(someKey, null));
    assertEquals(someDefaultValue, config.getProperty(someNonExistedKey, someDefaultValue));
  }

  @Test
  public void testFallbackOrderAcrossNamespaces() {
    newMockedConfigService();

    mockConfigs(someAppId, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, defaultNamespace,
            ImmutableMap.of("key.from.default", "value-default")));
    mockConfigs(FALLBACK_ANOTHER_APP_ID, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(FALLBACK_ANOTHER_APP_ID, defaultNamespace,
            ImmutableMap.of("key.from.another", "value-another")));
    mockConfigs(someAppId, someClusterName, PUBLIC_NAMESPACE, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, PUBLIC_NAMESPACE,
            ImmutableMap.of("key.from.public", "value-public")));

    Config appConfig = ConfigService.getAppConfig();
    Config anotherAppConfig = ConfigService.getConfig(FALLBACK_ANOTHER_APP_ID, defaultNamespace);
    Config publicConfig = ConfigService.getConfig(PUBLIC_NAMESPACE);

    assertEquals("value-default",
        resolveValueByFallbackOrder("key.from.default", appConfig, anotherAppConfig, publicConfig));
    assertEquals("value-another",
        resolveValueByFallbackOrder("key.from.another", appConfig, anotherAppConfig, publicConfig));
    assertEquals("value-public",
        resolveValueByFallbackOrder("key.from.public", appConfig, anotherAppConfig, publicConfig));
    assertEquals(DEFAULT_VALUE,
        resolveValueByFallbackOrder("key.unknown", appConfig, anotherAppConfig, publicConfig));
  }

  @Test
  public void testGetConfigWithSameNamespaceButDifferentAppIds() {
    newMockedConfigService();

    mockConfigs(someAppId, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, defaultNamespace,
            ImmutableMap.of(MULTI_APP_KEY, "value-from-default-app")));
    mockConfigs(MULTI_APP_ANOTHER_APP_ID, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(MULTI_APP_ANOTHER_APP_ID, defaultNamespace,
            ImmutableMap.of(MULTI_APP_KEY, "value-from-another-app")));

    Config defaultAppConfig = ConfigService.getConfig(someAppId, defaultNamespace);
    Config anotherAppConfig = ConfigService.getConfig(MULTI_APP_ANOTHER_APP_ID, defaultNamespace);

    assertEquals("value-from-default-app", defaultAppConfig.getProperty(MULTI_APP_KEY, null));
    assertEquals("value-from-another-app", anotherAppConfig.getProperty(MULTI_APP_KEY, null));
  }

  @Test
  public void testConfigChangeShouldOnlyAffectSpecifiedAppId() throws Exception {
    MockedConfigService mockedConfigService = newMockedConfigService();

    mockConfigs(someAppId, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, defaultNamespace,
            ImmutableMap.of(MULTI_APP_KEY, "default-v1")));
    mockConfigs(MULTI_APP_ANOTHER_APP_ID, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(MULTI_APP_ANOTHER_APP_ID, defaultNamespace,
            ImmutableMap.of(MULTI_APP_KEY, "another-v1")));
    mockedConfigService.mockLongPollNotifications(50, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(defaultNamespace, 1L)));

    Config defaultAppConfig = ConfigService.getConfig(someAppId, defaultNamespace);
    Config anotherAppConfig = ConfigService.getConfig(MULTI_APP_ANOTHER_APP_ID, defaultNamespace);

    assertEquals("default-v1", defaultAppConfig.getProperty(MULTI_APP_KEY, null));
    assertEquals("another-v1", anotherAppConfig.getProperty(MULTI_APP_KEY, null));

    SettableFuture<ConfigChangeEvent> defaultAppFuture = SettableFuture.create();
    SettableFuture<ConfigChangeEvent> anotherAppFuture = SettableFuture.create();

    defaultAppConfig.addChangeListener(futureListener(defaultAppFuture));
    anotherAppConfig.addChangeListener(futureListener(anotherAppFuture));

    mockConfigs(MULTI_APP_ANOTHER_APP_ID, someClusterName, defaultNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(MULTI_APP_ANOTHER_APP_ID, defaultNamespace,
            ImmutableMap.of(MULTI_APP_KEY, "another-v2")));

    mockedConfigService.mockLongPollNotifications(50, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(defaultNamespace, 2L)));

    ConfigChangeEvent anotherAppChangeEvent = anotherAppFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(anotherAppChangeEvent);
    assertEquals("another-v1", anotherAppChangeEvent.getChange(MULTI_APP_KEY).getOldValue());
    assertEquals("another-v2", anotherAppChangeEvent.getChange(MULTI_APP_KEY).getNewValue());

    assertEquals("default-v1", defaultAppConfig.getProperty(MULTI_APP_KEY, null));
    assertEquals("another-v2", anotherAppConfig.getProperty(MULTI_APP_KEY, null));
    assertNull(pollFuture(defaultAppFuture, 1000));
  }

  @Test
  public void testConfigFileWithPropertiesXmlAndYamlFormats() throws Exception {
    MockedConfigService mockedConfigService = newMockedConfigService();

    String propertiesNamespace = "application.properties";
    String xmlNamespace = "datasources.xml";
    String yamlNamespace = "application.yaml";

    mockConfigs(someAppId, someClusterName, propertiesNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, propertiesNamespace,
            ImmutableMap.of("timeout", "200", "batch", "100")));
    mockConfigs(someAppId, someClusterName, xmlNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, xmlNamespace,
            ImmutableMap.of(ConfigConsts.CONFIG_FILE_CONTENT_KEY,
                "<datasources><name>db-v1</name></datasources>")));
    mockConfigs(someAppId, someClusterName, yamlNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, yamlNamespace,
            ImmutableMap.of(ConfigConsts.CONFIG_FILE_CONTENT_KEY,
                "redis:\n  cache:\n    enabled: true\n    commandTimeout: 30\n")));
    mockedConfigService.mockLongPollNotifications(50, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(propertiesNamespace, 1L),
            new ApolloConfigNotification(xmlNamespace, 1L),
            new ApolloConfigNotification(yamlNamespace, 1L)));

    ConfigFile propertiesFile = ConfigService.getConfigFile("application", ConfigFileFormat.Properties);
    ConfigFile xmlFile = ConfigService.getConfigFile("datasources", ConfigFileFormat.XML);
    ConfigFile yamlFile = ConfigService.getConfigFile("application", ConfigFileFormat.YAML);

    assertTrue(propertiesFile instanceof PropertiesCompatibleConfigFile);
    Properties properties = ((PropertiesCompatibleConfigFile) propertiesFile).asProperties();
    assertEquals("200", properties.getProperty("timeout"));
    assertEquals("100", properties.getProperty("batch"));

    assertTrue(xmlFile.hasContent());
    assertEquals("<datasources><name>db-v1</name></datasources>", xmlFile.getContent());

    assertTrue(yamlFile instanceof PropertiesCompatibleConfigFile);
    Properties yamlProperties = ((PropertiesCompatibleConfigFile) yamlFile).asProperties();
    assertEquals("true", yamlProperties.getProperty("redis.cache.enabled"));
    assertEquals("30", yamlProperties.getProperty("redis.cache.commandTimeout"));

    SettableFuture<ConfigFileChangeEvent> xmlChangeFuture = SettableFuture.create();
    SettableFuture<ConfigFileChangeEvent> yamlChangeFuture = SettableFuture.create();

    xmlFile.addChangeListener(changeEvent -> {
      if (!xmlChangeFuture.isDone()) {
        xmlChangeFuture.set(changeEvent);
      }
    });
    yamlFile.addChangeListener(changeEvent -> {
      if (!yamlChangeFuture.isDone()) {
        yamlChangeFuture.set(changeEvent);
      }
    });

    mockConfigs(someAppId, someClusterName, xmlNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, xmlNamespace,
            ImmutableMap.of(ConfigConsts.CONFIG_FILE_CONTENT_KEY,
                "<datasources><name>db-v2</name></datasources>")));
    mockConfigs(someAppId, someClusterName, yamlNamespace, HttpServletResponse.SC_OK,
        assembleApolloConfigForApp(someAppId, yamlNamespace,
            ImmutableMap.of(ConfigConsts.CONFIG_FILE_CONTENT_KEY,
                "redis:\n  cache:\n    enabled: false\n    commandTimeout: 45\n")));

    mockedConfigService.mockLongPollNotifications(50, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(xmlNamespace, 2L),
            new ApolloConfigNotification(yamlNamespace, 2L)));

    ConfigFileChangeEvent xmlChange = xmlChangeFuture.get(5, TimeUnit.SECONDS);
    ConfigFileChangeEvent yamlChange = yamlChangeFuture.get(5, TimeUnit.SECONDS);

    assertEquals("datasources.xml", xmlChange.getNamespace());
    assertEquals(PropertyChangeType.MODIFIED, xmlChange.getChangeType());
    assertEquals("<datasources><name>db-v2</name></datasources>", xmlFile.getContent());

    assertEquals("application.yaml", yamlChange.getNamespace());
    assertEquals(PropertyChangeType.MODIFIED, yamlChange.getChangeType());
    Properties yamlPropertiesAfterRefresh = ((PropertiesCompatibleConfigFile) yamlFile)
        .asProperties();
    assertEquals("false", yamlPropertiesAfterRefresh.getProperty("redis.cache.enabled"));
    assertEquals("45", yamlPropertiesAfterRefresh.getProperty("redis.cache.commandTimeout"));
  }

  @Test
  public void testOrderGetConfigWithNoLocalFileButWithRemoteConfig() throws Exception {
    setPropertiesOrderEnabled(true);

    String someKey1 = "someKey1";
    String someValue1 = "someValue1";
    String someKey2 = "someKey2";
    String someValue2 = "someValue2";
    Map<String, String> configurations = new LinkedHashMap<>();
    configurations.put(someKey1, someValue1);
    configurations.put(someKey2, someValue2);
    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.copyOf(configurations));
    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    Set<String> propertyNames = config.getPropertyNames();
    Iterator<String> it = propertyNames.iterator();
    assertEquals(someKey1, it.next());
    assertEquals(someKey2, it.next());

  }

  @Test
  public void testGetConfigWithLocalFileAndWithRemoteConfig() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String anotherValue = "anotherValue";
    Properties properties = new Properties();
    properties.put(someKey, someValue);
    createLocalCachePropertyFile(properties);

    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.of(someKey, anotherValue));
    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    assertEquals(anotherValue, config.getProperty(someKey, null));
  }

  @Test
  public void testOrderGetConfigWithLocalFileAndWithRemoteConfig() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String anotherValue = "anotherValue";

    String someKey1 = "someKey1";
    String someValue1 = "someValue1";
    String anotherValue1 = "anotherValue1";
    String someKey2 = "someKey2";
    String someValue2 = "someValue2";

    setPropertiesOrderEnabled(true);

    Properties properties = new OrderedProperties();
    properties.put(someKey, someValue);
    properties.put(someKey1, someValue1);
    properties.put(someKey2, someValue2);
    createLocalCachePropertyFile(properties);

    Map<String, String> configurations = new LinkedHashMap<>();
    configurations.put(someKey, anotherValue);
    configurations.put(someKey1, anotherValue1);
    configurations.put(someKey2, someValue2);
    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.copyOf(configurations));

    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    assertEquals(anotherValue, config.getProperty(someKey, null));

    Set<String> propertyNames = config.getPropertyNames();
    Iterator<String> it = propertyNames.iterator();
    assertEquals(someKey, it.next());
    assertEquals(someKey1, it.next());
    assertEquals(someKey2, it.next());
    assertEquals(anotherValue1, config.getProperty(someKey1, ""));

  }

  @Test
  public void testGetConfigWithNoLocalFileAndRemoteConfigError() throws Exception {

    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);

    Config config = ConfigService.getAppConfig();

    String someKey = "someKey";
    String someDefaultValue = "defaultValue" + Math.random();

    assertEquals(someDefaultValue, config.getProperty(someKey, someDefaultValue));
  }

  @Test
  public void testGetConfigWithLocalFileAndRemoteConfigError() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    Properties properties = new Properties();
    properties.put(someKey, someValue);
    createLocalCachePropertyFile(properties);

    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);

    Config config = ConfigService.getAppConfig();
    assertEquals(someValue, config.getProperty(someKey, null));
  }

  @Test
  public void testOrderGetConfigWithLocalFileAndRemoteConfigError() throws Exception {
    String someKey1 = "someKey1";
    String someValue1 = "someValue1";
    String someKey2 = "someKey2";
    String someValue2 = "someValue2";

    setPropertiesOrderEnabled(true);

    Properties properties = new OrderedProperties();
    properties.put(someKey1, someValue1);
    properties.put(someKey2, someValue2);
    createLocalCachePropertyFile(properties);

    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);


    Config config = ConfigService.getAppConfig();
    assertEquals(someValue1, config.getProperty(someKey1, null));
    assertEquals(someValue2, config.getProperty(someKey2, null));

    Set<String> propertyNames = config.getPropertyNames();
    Iterator<String> it = propertyNames.iterator();
    assertEquals(someKey1, it.next());
    assertEquals(someKey2, it.next());
  }

  @Test
  public void testGetConfigWithNoLocalFileAndRemoteMetaServiceRetry() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.of(someKey, someValue));
    boolean failAtFirstTime = true;

    newMockedConfigService();
    mockMetaServer(failAtFirstTime);
    mockConfigs(failAtFirstTime, HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    assertEquals(someValue, config.getProperty(someKey, null));
  }

  @Test
  public void testGetConfigWithNoLocalFileAndRemoteConfigServiceRetry() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    ApolloConfig apolloConfig = assembleApolloConfig(ImmutableMap.of(someKey, someValue));
    boolean failedAtFirstTime = true;

    newMockedConfigService();
    mockConfigs(failedAtFirstTime, HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();

    assertEquals(someValue, config.getProperty(someKey, null));
  }

  @Test
  public void testRefreshConfig() throws Exception {
    final String someKey = "someKey";
    final String someValue = "someValue";
    final String anotherValue = "anotherValue";

    int someRefreshInterval = 500;
    TimeUnit someRefreshTimeUnit = TimeUnit.MILLISECONDS;

    setRefreshInterval(someRefreshInterval);
    setRefreshTimeUnit(someRefreshTimeUnit);

    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig apolloConfig = assembleApolloConfig(configurations);

    newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();
    final List<ConfigChangeEvent> changeEvents = Lists.newArrayList();

    final SettableFuture<Boolean> refreshFinished = SettableFuture.create();
    config.addChangeListener(new ConfigChangeListener() {
      AtomicInteger counter = new AtomicInteger(0);

      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        //only need to assert once
        if (counter.incrementAndGet() > 1) {
          return;
        }
        assertEquals(1, changeEvent.changedKeys().size());
        assertTrue(changeEvent.isChanged(someKey));
        assertEquals(someValue, changeEvent.getChange(someKey).getOldValue());
        assertEquals(anotherValue, changeEvent.getChange(someKey).getNewValue());
        // if there is any assertion failed above, this line won't be executed
        changeEvents.add(changeEvent);
        refreshFinished.set(true);
      }
    });

    apolloConfig.getConfigurations().put(someKey, anotherValue);
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    refreshFinished.get(someRefreshInterval * 5, someRefreshTimeUnit);

    assertEquals(1, changeEvents.size(),
        "Change event's size should equal to one or there must be some assertion failed in change listener");
    assertEquals(anotherValue, config.getProperty(someKey, null));
  }

  @Test
  public void testLongPollRefresh() throws Exception {
    final String someKey = "someKey";
    final String someValue = "someValue";
    final String anotherValue = "anotherValue";
    long someNotificationId = 1;

    long pollTimeoutInMS = 50;
    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig apolloConfig = assembleApolloConfig(configurations);

    MockedConfigService mockedConfigService = newMockedConfigService();
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);
    mockedConfigService.mockLongPollNotifications(pollTimeoutInMS, HttpServletResponse.SC_OK,
            Lists.newArrayList(
                new ApolloConfigNotification(apolloConfig.getNamespaceName(), someNotificationId))
        );

    Config config = ConfigService.getAppConfig();
    assertEquals(someValue, config.getProperty(someKey, null));

    final SettableFuture<Boolean> longPollFinished = SettableFuture.create();

    config.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        longPollFinished.set(true);
      }
    });

    apolloConfig.getConfigurations().put(someKey, anotherValue);
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    longPollFinished.get(pollTimeoutInMS * 20, TimeUnit.MILLISECONDS);

    assertEquals(anotherValue, config.getProperty(someKey, null));
  }

  @Test
  public void testLongPollRefreshWithMultipleNamespacesAndOnlyOneNamespaceNotified()
      throws Exception {
    final String someKey = "someKey";
    final String someValue = "someValue";
    final String anotherValue = "anotherValue";
    long someNotificationId = 1;

    long pollTimeoutInMS = 50;
    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig apolloConfig = assembleApolloConfig(configurations);

    MockedConfigService mockedConfigService = newMockedConfigService();
    mockConfigs(true, HttpServletResponse.SC_OK, apolloConfig);

    Config someOtherConfig = ConfigService.getConfig(someOtherNamespace);
    Config config = ConfigService.getAppConfig();
    assertEquals(someValue, config.getProperty(someKey, null));
    assertEquals(someValue, someOtherConfig.getProperty(someKey, null));

    final SettableFuture<Boolean> longPollFinished = SettableFuture.create();

    // add change listener first
    config.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        longPollFinished.set(true);
      }
    });

    // change the config on remote
    apolloConfig.getConfigurations().put(someKey, anotherValue);
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    // notify
    mockedConfigService.mockLongPollNotifications(
        false, pollTimeoutInMS, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(apolloConfig.getNamespaceName(), someNotificationId))
    );

    longPollFinished.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(anotherValue, config.getProperty(someKey, null));

    TimeUnit.MILLISECONDS.sleep(pollTimeoutInMS * 10);
    assertEquals(someValue, someOtherConfig.getProperty(someKey, null));
  }

  @Test
  public void testLongPollRefreshWithMultipleNamespacesAndMultipleNamespaceNotified()
      throws Exception {
    final String someKey = "someKey";
    final String someValue = "someValue";
    final String anotherValue = "anotherValue";
    long someNotificationId = 1;

    long pollTimeoutInMS = 50;
    Map<String, String> configurations = Maps.newHashMap();
    configurations.put(someKey, someValue);
    ApolloConfig apolloConfig = assembleApolloConfig(configurations);
    MockedConfigService mockedConfigService = newMockedConfigService();
    mockConfigs(true, HttpServletResponse.SC_OK, apolloConfig);

    Config config = ConfigService.getAppConfig();
    Config someOtherConfig = ConfigService.getConfig(someOtherNamespace);
    assertEquals(someValue, config.getProperty(someKey, null));
    assertEquals(someValue, someOtherConfig.getProperty(someKey, null));

    final SettableFuture<Boolean> longPollFinished = SettableFuture.create();
    final SettableFuture<Boolean> someOtherNamespacelongPollFinished = SettableFuture.create();

    // add change listener first
    config.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        longPollFinished.set(true);
      }
    });
    someOtherConfig.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        someOtherNamespacelongPollFinished.set(true);
      }
    });

    // change the config on remote
    apolloConfig.getConfigurations().put(someKey, anotherValue);
    mockConfigs(HttpServletResponse.SC_OK, apolloConfig);

    // notify
    mockedConfigService.mockLongPollNotifications(
        false, pollTimeoutInMS, HttpServletResponse.SC_OK,
        Lists.newArrayList(
            new ApolloConfigNotification(apolloConfig.getNamespaceName(), someNotificationId),
            new ApolloConfigNotification(someOtherNamespace, someNotificationId))
    );

    longPollFinished.get(5000, TimeUnit.MILLISECONDS);
    someOtherNamespacelongPollFinished.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(anotherValue, config.getProperty(someKey, null));
    assertEquals(anotherValue, someOtherConfig.getProperty(someKey, null));

  }

  private ApolloConfig assembleApolloConfig(Map<String, String> configurations) {
    return assembleApolloConfigForApp(someAppId, defaultNamespace, configurations);
  }

  private ApolloConfig assembleApolloConfigForApp(
      String appId, String namespace, Map<String, String> configurations) {
    ApolloConfig apolloConfig =
        new ApolloConfig(appId, someClusterName, namespace, someReleaseKey);
    apolloConfig.setConfigurations(configurations);
    return apolloConfig;
  }

  private String resolveValueByFallbackOrder(
      String key,
      Config appConfig,
      Config anotherAppConfig,
      Config publicConfig) {
    String value = appConfig.getProperty(key, DEFAULT_VALUE);
    if (DEFAULT_VALUE.equals(value)) {
      value = anotherAppConfig.getProperty(key, DEFAULT_VALUE);
    }
    if (DEFAULT_VALUE.equals(value)) {
      value = publicConfig.getProperty(key, DEFAULT_VALUE);
    }
    return value;
  }

  private ConfigChangeListener futureListener(SettableFuture<ConfigChangeEvent> future) {
    return changeEvent -> {
      if (!future.isDone()) {
        future.set(changeEvent);
      }
    };
  }

  private ConfigChangeEvent pollFuture(SettableFuture<ConfigChangeEvent> future, long timeoutInMs)
      throws Exception {
    try {
      return future.get(timeoutInMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ignore) {
      return null;
    }
  }

}
