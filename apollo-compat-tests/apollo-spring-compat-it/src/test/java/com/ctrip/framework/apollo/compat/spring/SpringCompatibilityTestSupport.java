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
package com.ctrip.framework.apollo.compat.spring;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.google.common.collect.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;

final class SpringCompatibilityTestSupport {

  private static final String ORIGINAL_APP_ID = System.getProperty("app.id");
  private static final String ORIGINAL_ENV = System.getProperty("env");

  private SpringCompatibilityTestSupport() {
  }

  static void beforeClass(EmbeddedApollo embeddedApollo) throws Exception {
    System.setProperty("app.id", "someAppId");
    System.setProperty("env", "local");
    embeddedApollo.resetOverriddenProperties();
    resetApolloState();
  }

  static void afterClass() throws Exception {
    restoreOrClear("app.id", ORIGINAL_APP_ID);
    restoreOrClear("env", ORIGINAL_ENV);
    resetApolloState();
  }

  static void waitForCondition(String failureMessage, Callable<Boolean> condition) throws Exception {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
    while (System.currentTimeMillis() < deadline) {
      if (Boolean.TRUE.equals(condition.call())) {
        return;
      }
      TimeUnit.MILLISECONDS.sleep(100);
    }
    throw new AssertionError(failureMessage);
  }

  static Properties copyConfigProperties(Config config) {
    Properties properties = new Properties();
    for (String key : config.getPropertyNames()) {
      properties.setProperty(key, config.getProperty(key, ""));
    }
    return properties;
  }

  static void applyConfigChange(Config config, String namespace, Properties properties) {
    Assert.assertTrue(config instanceof DefaultConfig);
    ((DefaultConfig) config).onRepositoryChange(namespace, properties);
  }

  static void applyConfigChange(Config config, String appId, String namespace,
      Properties properties) {
    Assert.assertTrue(config instanceof DefaultConfig);
    ((DefaultConfig) config).onRepositoryChange(appId, namespace, properties);
  }

  private static void restoreOrClear(String key, String originalValue) {
    if (originalValue == null) {
      System.clearProperty(key);
      return;
    }
    System.setProperty(key, originalValue);
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
}
