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
package com.ctrip.framework.apollo.config.data.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.config.data.injector.ApolloConfigDataInjectorCustomizer;
import com.ctrip.framework.apollo.config.data.injector.ApolloMockInjectorCustomizer;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.google.common.collect.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ConfigDataIntegrationTest.TestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "app.id=someAppId",
        "env=local",
        "spring.config.import=apollo://application,apollo://TEST1.apollo,apollo://application.yaml",
        "listeners=application,TEST1.apollo,application.yaml"
    })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigDataIntegrationTest {

  private static final String TEST_APP_ID = "someAppId";
  private static final String TEST_ENV = "local";

  private static final EmbeddedApollo embeddedApollo = new EmbeddedApollo();

  private static final ExternalResource apolloStateResource = new ExternalResource() {
    private String originalAppId;
    private String originalEnv;

    @Override
    protected void before() throws Throwable {
      originalAppId = System.getProperty("app.id");
      originalEnv = System.getProperty("env");
      System.setProperty("app.id", TEST_APP_ID);
      System.setProperty("env", TEST_ENV);
      resetApolloStaticState();
    }

    @Override
    protected void after() {
      try {
        resetApolloStaticState();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      } finally {
        restoreOrClear("app.id", originalAppId);
        restoreOrClear("env", originalEnv);
      }
    }
  };

  @ClassRule
  public static final RuleChain apolloRuleChain = RuleChain
      .outerRule(apolloStateResource)
      .around(embeddedApollo);

  @Before
  public void beforeEach() {
    embeddedApollo.resetOverriddenProperties();
  }

  @After
  public void afterEach() throws Exception {
    resetApolloStaticState();
  }

  @Autowired
  private Environment environment;

  @Autowired(required = false)
  private FeatureEnabledBean featureEnabledBean;

  @Autowired
  private ListenerProbe listenerProbe;

  @Autowired
  private RedisCacheProperties redisCacheProperties;

  @Test
  public void testImportMultipleNamespacesAndConditionalOnProperty() {
    assertEquals("ok", environment.getProperty("application.only"));
    assertEquals("ok", environment.getProperty("test1.only"));
    assertEquals("ok", environment.getProperty("yaml.only"));
    assertEquals("from-yaml", environment.getProperty("priority.value"));
    assertNotNull(featureEnabledBean);
    assertTrue(redisCacheProperties.isEnabled());
    assertEquals(35, redisCacheProperties.getCommandTimeout());
  }

  @Test
  public void testApolloConfigChangeListenerWithInterestedKeyPrefixes() throws Exception {
    assertEquals("35", environment.getProperty("redis.cache.commandTimeout"));

    addOrModifyForAllAppIds("application", "redis.cache.commandTimeout", "45");
    ConfigChangeEvent interestedEvent = listenerProbe.pollEvent(10, TimeUnit.SECONDS);
    assertNotNull(interestedEvent);
    assertTrue(interestedEvent.changedKeys().contains("redis.cache.commandTimeout"));
    assertEquals(45, ConfigService.getConfig("application")
        .getIntProperty("redis.cache.commandTimeout", -1).intValue());

    addOrModifyForAllAppIds("application.yaml", ConfigConsts.CONFIG_FILE_CONTENT_KEY,
        "priority:\n  value: from-yaml\nyaml:\n  only: ok\nredis:\n  cache:\n    commandTimeout: 55\n");
    ConfigChangeEvent yamlInterestedEvent = listenerProbe.pollEvent(10, TimeUnit.SECONDS);
    assertNotNull(yamlInterestedEvent);
    assertTrue(yamlInterestedEvent.changedKeys().contains("redis.cache.commandTimeout"));
    assertEquals("55", environment.getProperty("redis.cache.commandTimeout"));

    addOrModifyForAllAppIds("application", "apollo.unrelated.key", "value");
    ConfigChangeEvent unrelatedEvent = listenerProbe.pollEvent(3000, TimeUnit.MILLISECONDS);
    assertNull(unrelatedEvent);
  }

  @EnableAutoConfiguration
  @EnableConfigurationProperties(RedisCacheProperties.class)
  @Configuration
  static class TestConfiguration {

    @Bean
    @ConditionalOnProperty(value = "feature.enabled", havingValue = "true")
    public FeatureEnabledBean featureEnabledBean() {
      return new FeatureEnabledBean();
    }

    @Bean
    public ListenerProbe listenerProbe() {
      return new ListenerProbe();
    }
  }

  static class FeatureEnabledBean {
  }

  @ConfigurationProperties(prefix = "redis.cache")
  static class RedisCacheProperties {

    private boolean enabled;
    private int commandTimeout;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getCommandTimeout() {
      return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
      this.commandTimeout = commandTimeout;
    }
  }

  static class ListenerProbe {

    private final BlockingQueue<ConfigChangeEvent> queue = new ArrayBlockingQueue<>(10);

    @ApolloConfigChangeListener(value = "${listeners}",
        interestedKeyPrefixes = {"redis.cache."})
    private void onChange(ConfigChangeEvent changeEvent) {
      queue.offer(changeEvent);
    }

    ConfigChangeEvent pollEvent(long timeout, TimeUnit unit) throws InterruptedException {
      return queue.poll(timeout, unit);
    }
  }

  private static void resetApolloStaticState() throws Exception {
    ApolloMockInjectorCustomizer.clear();

    Field instanceSuppliers =
        ApolloConfigDataInjectorCustomizer.class.getDeclaredField("INSTANCE_SUPPLIERS");
    instanceSuppliers.setAccessible(true);
    ((Map<?, ?>) instanceSuppliers.get(null)).clear();

    Field instances = ApolloConfigDataInjectorCustomizer.class.getDeclaredField("INSTANCES");
    instances.setAccessible(true);
    ((Map<?, ?>) instances.get(null)).clear();

    Class<?> initializerClass = Class.forName(
        "com.ctrip.framework.apollo.config.data.importer.ApolloConfigDataLoaderInitializer");
    Field initialized = initializerClass.getDeclaredField("INITIALIZED");
    initialized.setAccessible(true);
    initialized.setBoolean(null, false);

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

  @SuppressWarnings("unchecked")
  private static void clearField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object container = field.get(target);
    if (container instanceof Map) {
      ((Map<?, ?>) container).clear();
      return;
    }
    if (container instanceof Table) {
      ((Table<?, ?, ?>) container).clear();
      return;
    }
    Method clearMethod = container.getClass().getDeclaredMethod("clear");
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

  private static void addOrModifyForAllAppIds(String namespace, String key, String value) {
    embeddedApollo.addOrModifyProperty(TEST_APP_ID, namespace, key, value);
    embeddedApollo.addOrModifyProperty(
        ConfigConsts.NO_APPID_PLACEHOLDER, namespace, key, value);
  }

}
