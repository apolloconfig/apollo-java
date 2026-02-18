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
package com.ctrip.framework.apollo.compat.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class ApolloApiCompatibilityTest {

  @ClassRule
  public static final EmbeddedApollo EMBEDDED_APOLLO = new EmbeddedApollo();

  private static final String SOME_APP_ID = "someAppId";
  private static final String ANOTHER_APP_ID = "100004459";
  private static final String DEFAULT_VALUE = "undefined";

  private static final String ORIGINAL_APP_ID = System.getProperty("app.id");
  private static final String ORIGINAL_ENV = System.getProperty("env");

  static {
    System.setProperty("app.id", SOME_APP_ID);
    System.setProperty("env", "local");
  }

  @Before
  public void setUp() throws Exception {
    EMBEDDED_APOLLO.resetOverriddenProperties();
    resetApolloState();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    resetApolloState();
    restoreOrClear("app.id", ORIGINAL_APP_ID);
    restoreOrClear("env", ORIGINAL_ENV);
  }

  @Test
  public void shouldLoadConfigsWithFallbackInNoSpringRuntime() {
    assertClassNotPresent("org.springframework.context.ApplicationContext");

    Config appConfig = ConfigService.getAppConfig();
    Config anotherAppConfig = ConfigService.getConfig(ANOTHER_APP_ID, "application");
    Config publicConfig = ConfigService.getConfig("TEST1.apollo");
    Config yamlConfig = ConfigService.getConfig("application.yaml");

    assertEquals("from-default-app",
        resolveValueByFallback("primary.key", appConfig, anotherAppConfig, publicConfig, yamlConfig));
    assertEquals("from-another-app",
        resolveValueByFallback("fallback.only", appConfig, anotherAppConfig, publicConfig, yamlConfig));
    assertEquals("from-public-namespace",
        resolveValueByFallback("public.only", appConfig, anotherAppConfig, publicConfig, yamlConfig));
    assertEquals("from-yaml-namespace",
        resolveValueByFallback("yaml.only", appConfig, anotherAppConfig, publicConfig, yamlConfig));
    assertEquals(DEFAULT_VALUE,
        resolveValueByFallback("missing.key", appConfig, anotherAppConfig, publicConfig, yamlConfig));
  }

  @Test
  public void shouldIsolateListenersForDifferentAppIdsInNoSpringRuntime() throws Exception {
    Config defaultConfig = ConfigService.getConfig(SOME_APP_ID, "application");
    Config anotherAppConfig = ConfigService.getConfig(ANOTHER_APP_ID, "application");

    SettableFuture<ConfigChangeEvent> defaultFuture = SettableFuture.create();
    SettableFuture<ConfigChangeEvent> anotherFuture = SettableFuture.create();

    defaultConfig.addChangeListener(changeEvent -> {
      if (!defaultFuture.isDone()) {
        defaultFuture.set(changeEvent);
      }
    });
    anotherAppConfig.addChangeListener(changeEvent -> {
      if (!anotherFuture.isDone()) {
        anotherFuture.set(changeEvent);
      }
    });

    EMBEDDED_APOLLO.addOrModifyProperty(ANOTHER_APP_ID, "application", "fallback.only", "another-updated");

    ConfigChangeEvent anotherChangeEvent = anotherFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(anotherChangeEvent.getChange("fallback.only"));
    assertEquals("from-another-app", anotherChangeEvent.getChange("fallback.only").getOldValue());
    assertEquals("another-updated", anotherChangeEvent.getChange("fallback.only").getNewValue());

    assertNull(pollFuture(defaultFuture, 300));
    assertEquals("from-default-app", defaultConfig.getProperty("primary.key", null));
    assertEquals("another-updated", anotherAppConfig.getProperty("fallback.only", null));
  }

  @Test
  public void shouldLoadConfigFilesInNoSpringRuntime() {
    ConfigFile xmlConfigFile = ConfigService.getConfigFile("datasources", ConfigFileFormat.XML);
    ConfigFile yamlConfigFile = ConfigService.getConfigFile("application", ConfigFileFormat.YAML);

    assertEquals("<datasources><name>db-v1</name></datasources>", xmlConfigFile.getContent());

    Properties yamlProperties = ((PropertiesCompatibleConfigFile) yamlConfigFile).asProperties();
    assertEquals("from-yaml-namespace", yamlProperties.getProperty("yaml.only"));
    assertEquals("35", yamlProperties.getProperty("redis.cache.commandTimeout"));
  }

  private static String resolveValueByFallback(String key, Config appConfig, Config anotherAppConfig,
      Config publicConfig, Config yamlConfig) {
    String value = appConfig.getProperty(key, DEFAULT_VALUE);
    if (!DEFAULT_VALUE.equals(value)) {
      return value;
    }

    value = anotherAppConfig.getProperty(key, DEFAULT_VALUE);
    if (!DEFAULT_VALUE.equals(value)) {
      return value;
    }

    value = publicConfig.getProperty(key, DEFAULT_VALUE);
    if (!DEFAULT_VALUE.equals(value)) {
      return value;
    }

    return yamlConfig.getProperty(key, DEFAULT_VALUE);
  }

  private static <T> T pollFuture(SettableFuture<T> future, long timeoutMillis) throws Exception {
    try {
      return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      return null;
    }
  }

  private static void assertClassNotPresent(String className) {
    try {
      Class.forName(className);
      fail("Class should not be present: " + className);
    } catch (ClassNotFoundException ignored) {
      // ignore
    }
  }

  private static void resetApolloState() throws Exception {
    Method resetMethod = ConfigService.class.getDeclaredMethod("reset");
    resetMethod.setAccessible(true);
    resetMethod.invoke(null);
    clearApolloClientCaches();
  }

  private static void clearApolloClientCaches() throws Exception {
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configs");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configLocks");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFiles");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFileLocks");
    clearField(ApolloInjector.getInstance(ConfigFactoryManager.class), "m_factories");
    clearField(ApolloInjector.getInstance(ConfigRegistry.class), "m_instances");
  }

  private static void clearField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object container = field.get(target);
    if (container == null) {
      return;
    }
    if (container instanceof Map) {
      ((Map<?, ?>) container).clear();
      return;
    }
    if (container instanceof Table) {
      ((Table<?, ?, ?>) container).clear();
      return;
    }
    Method clearMethod = container.getClass().getMethod("clear");
    clearMethod.setAccessible(true);
    clearMethod.invoke(container);
  }

  private static void restoreOrClear(String key, String originalValue) {
    if (originalValue == null) {
      System.clearProperty(key);
      return;
    }
    System.setProperty(key, originalValue);
  }
}
