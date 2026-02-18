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
package com.ctrip.framework.apollo.compat.springboot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.MultipleConfig;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.google.common.collect.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApolloSpringBootCompatibilityTest.TestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "app.id=someAppId",
        "env=local",
        "spring.config.import=apollo://application,apollo://TEST1.apollo,apollo://application.yaml",
        "listeners=application,TEST1.apollo,application.yaml",
        "org.springframework.boot.logging.LoggingSystem=none"
    })
@DirtiesContext
public class ApolloSpringBootCompatibilityTest {

  private static final String SOME_APP_ID = "someAppId";
  private static final String ANOTHER_APP_ID = "100004459";

  @ClassRule
  public static final EmbeddedApollo EMBEDDED_APOLLO = new EmbeddedApollo();

  @Autowired
  private Environment environment;

  @Autowired(required = false)
  private FeatureBean featureBean;

  @Autowired
  private RedisCacheProperties redisCacheProperties;

  @Autowired
  private CompatAnnotatedBean compatAnnotatedBean;

  @Autowired
  private ApolloApplicationListenerProbe applicationListenerProbe;

  @BeforeClass
  public static void beforeClass() throws Exception {
    EMBEDDED_APOLLO.resetOverriddenProperties();
    resetApolloState();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    resetApolloState();
  }

  @Test
  public void shouldCoverSpringBootDemoScenarios() throws Exception {
    assertEquals("boot-compat", environment.getProperty("yaml.marker"));
    assertNotNull(featureBean);
    assertTrue(redisCacheProperties.isEnabled());
    assertEquals(40, redisCacheProperties.getCommandTimeout());

    assertEquals(800, compatAnnotatedBean.getTimeout());
    assertEquals("from-public-boot", compatAnnotatedBean.getPublicOnly());
    assertEquals("from-another-app-boot",
        compatAnnotatedBean.getAnotherAppConfig().getProperty("compat.origin", null));
    assertEquals("from-public-boot",
        compatAnnotatedBean.getPublicNamespaceConfig().getProperty("public.only", null));
    assertEquals("boot-compat",
        compatAnnotatedBean.getYamlNamespaceConfig().getProperty("yaml.marker", null));
    assertEquals("800",
        compatAnnotatedBean.getApplicationConfig().getProperty("compat.timeout", null));
    assertEquals(2, compatAnnotatedBean.getJsonBeans().size());
    assertEquals("alpha-boot", compatAnnotatedBean.getJsonBeans().get(0).getSomeString());

    Config applicationConfig = ConfigService.getConfig("application");
    Properties applicationProperties = copyConfigProperties(applicationConfig);
    applicationProperties.setProperty("compat.timeout", "801");
    applicationProperties.setProperty("jsonBeanProperty",
        "[{\"someString\":\"gamma-boot\",\"someInt\":303}]");
    applyConfigChange(applicationConfig, SOME_APP_ID, "application", applicationProperties);

    Config publicConfig = ConfigService.getConfig("TEST1.apollo");
    Properties publicProperties = copyConfigProperties(publicConfig);
    publicProperties.setProperty("public.only", "from-public-boot-updated");
    applyConfigChange(publicConfig, SOME_APP_ID, "TEST1.apollo", publicProperties);

    Config yamlConfig = ConfigService.getConfig("application.yaml");
    Properties yamlProperties = copyConfigProperties(yamlConfig);
    yamlProperties.setProperty("yaml.marker", "boot-compat-updated");
    applyConfigChange(yamlConfig, SOME_APP_ID, "application.yaml", yamlProperties);

    Properties anotherAppProperties = copyConfigProperties(compatAnnotatedBean.getAnotherAppConfig());
    anotherAppProperties.setProperty("compat.origin", "changed-origin-boot");
    applyConfigChange(compatAnnotatedBean.getAnotherAppConfig(), ANOTHER_APP_ID, "application",
        anotherAppProperties);

    ConfigChangeEvent defaultChange = compatAnnotatedBean.pollDefaultEvent(5, TimeUnit.SECONDS);
    assertNotNull(defaultChange);
    assertNotNull(defaultChange.getChange("compat.timeout"));

    ConfigChangeEvent publicChange = compatAnnotatedBean.pollPublicNamespaceEvent(5, TimeUnit.SECONDS);
    assertNotNull(publicChange);
    assertNotNull(publicChange.getChange("public.only"));

    ConfigChangeEvent yamlChange = compatAnnotatedBean.pollYamlNamespaceEvent(5, TimeUnit.SECONDS);
    assertNotNull(yamlChange);
    assertNotNull(yamlChange.getChange("yaml.marker"));

    ConfigChangeEvent anotherAppChange = compatAnnotatedBean.pollAnotherAppEvent(5, TimeUnit.SECONDS);
    assertNotNull(anotherAppChange);
    assertNotNull(anotherAppChange.getChange("compat.origin"));

    waitForCondition("another app config should be updated",
        () -> "changed-origin-boot".equals(
            compatAnnotatedBean.getAnotherAppConfig().getProperty("compat.origin", null)));
    waitForCondition("public namespace config should be updated",
        () -> "from-public-boot-updated".equals(
            compatAnnotatedBean.getPublicNamespaceConfig().getProperty("public.only", null)));
    waitForCondition("yaml namespace config should be updated",
        () -> "boot-compat-updated".equals(
            compatAnnotatedBean.getYamlNamespaceConfig().getProperty("yaml.marker", null)));
    waitForCondition("application namespace config should be updated",
        () -> "801".equals(
            compatAnnotatedBean.getApplicationConfig().getProperty("compat.timeout", null)));
    waitForCondition("json value should be updated",
        () -> compatAnnotatedBean.getJsonBeans().size() == 1
            && "gamma-boot".equals(compatAnnotatedBean.getJsonBeans().get(0).getSomeString()));

    waitForCondition("ApplicationListener should receive namespace updates",
        () -> applicationListenerProbe.hasNamespace("application")
            && applicationListenerProbe.hasNamespace("TEST1.apollo")
            && applicationListenerProbe.hasNamespace("application.yaml"));
  }

  @EnableAutoConfiguration
  @EnableApolloConfig(value = {"application", "TEST1.apollo", "application.yaml"},
      multipleConfigs = {
          @MultipleConfig(appId = ANOTHER_APP_ID, namespaces = {"application"}, order = 9)
      })
  @EnableConfigurationProperties(RedisCacheProperties.class)
  @Configuration
  static class TestConfiguration {

    @Bean
    @ConditionalOnProperty(value = "feature.enabled", havingValue = "true")
    public FeatureBean featureBean() {
      return new FeatureBean();
    }

    @Bean
    public CompatAnnotatedBean compatAnnotatedBean() {
      return new CompatAnnotatedBean();
    }

    @Bean
    public ApolloApplicationListenerProbe apolloApplicationListenerProbe() {
      return new ApolloApplicationListenerProbe();
    }

  }

  static class FeatureBean {
  }

  @ConfigurationProperties(prefix = "redis.cache")
  static class RedisCacheProperties {

    private boolean enabled;
    private int commandTimeout;
    private int expireSeconds;
    private String clusterNodes;

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

    public int getExpireSeconds() {
      return expireSeconds;
    }

    public void setExpireSeconds(int expireSeconds) {
      this.expireSeconds = expireSeconds;
    }

    public String getClusterNodes() {
      return clusterNodes;
    }

    public void setClusterNodes(String clusterNodes) {
      this.clusterNodes = clusterNodes;
    }
  }

  public static class CompatAnnotatedBean {

    private final BlockingQueue<ConfigChangeEvent> defaultEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> publicNamespaceEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> yamlNamespaceEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> anotherAppEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);

    private volatile int timeout;
    private volatile String publicOnly;
    private volatile List<JsonBean> jsonBeans = Collections.emptyList();

    @ApolloConfig
    private Config applicationConfig;

    @ApolloConfig("TEST1.apollo")
    private Config publicNamespaceConfig;

    @ApolloConfig("application.yaml")
    private Config yamlNamespaceConfig;

    @ApolloConfig(appId = ANOTHER_APP_ID)
    private Config anotherAppConfig;

    @Value("${compat.timeout:0}")
    public void setTimeout(int timeout) {
      this.timeout = timeout;
    }

    @Value("${public.only:missing}")
    public void setPublicOnly(String publicOnly) {
      this.publicOnly = publicOnly;
    }

    @ApolloJsonValue("${jsonBeanProperty:[]}")
    public void setJsonBeans(List<JsonBean> jsonBeans) {
      this.jsonBeans = jsonBeans;
    }

    @ApolloConfigChangeListener(value = "application", interestedKeyPrefixes = {"compat."})
    private void onDefaultNamespaceChange(ConfigChangeEvent event) {
      defaultEvents.offer(event);
    }

    @ApolloConfigChangeListener(value = "TEST1.apollo", interestedKeyPrefixes = {"public."})
    private void onPublicNamespaceChange(ConfigChangeEvent event) {
      publicNamespaceEvents.offer(event);
    }

    @ApolloConfigChangeListener(value = "application.yaml", interestedKeyPrefixes = {"yaml."})
    private void onYamlNamespaceChange(ConfigChangeEvent event) {
      yamlNamespaceEvents.offer(event);
    }

    @ApolloConfigChangeListener(appId = ANOTHER_APP_ID,
        interestedKeyPrefixes = {"compat.origin"})
    private void onAnotherAppChange(ConfigChangeEvent event) {
      anotherAppEvents.offer(event);
    }

    int getTimeout() {
      return timeout;
    }

    String getPublicOnly() {
      return publicOnly;
    }

    List<JsonBean> getJsonBeans() {
      return jsonBeans;
    }

    Config getAnotherAppConfig() {
      return anotherAppConfig;
    }

    Config getApplicationConfig() {
      return applicationConfig;
    }

    Config getPublicNamespaceConfig() {
      return publicNamespaceConfig;
    }

    Config getYamlNamespaceConfig() {
      return yamlNamespaceConfig;
    }

    ConfigChangeEvent pollDefaultEvent(long timeout, TimeUnit unit) throws InterruptedException {
      return defaultEvents.poll(timeout, unit);
    }

    ConfigChangeEvent pollPublicNamespaceEvent(long timeout, TimeUnit unit) throws InterruptedException {
      return publicNamespaceEvents.poll(timeout, unit);
    }

    ConfigChangeEvent pollYamlNamespaceEvent(long timeout, TimeUnit unit) throws InterruptedException {
      return yamlNamespaceEvents.poll(timeout, unit);
    }

    ConfigChangeEvent pollAnotherAppEvent(long timeout, TimeUnit unit) throws InterruptedException {
      return anotherAppEvents.poll(timeout, unit);
    }
  }

  static class ApolloApplicationListenerProbe implements ApplicationListener {

    private final Set<String> changes = Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> namespaces = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      if (event instanceof ApolloConfigChangeEvent) {
        ConfigChangeEvent configChangeEvent = ((ApolloConfigChangeEvent) event).getConfigChangeEvent();
        changes.add(configChangeEvent.getAppId() + "#" + configChangeEvent.getNamespace());
        namespaces.add(configChangeEvent.getNamespace());
      }
    }

    boolean hasReceived(String marker) {
      return changes.contains(marker);
    }

    boolean hasNamespace(String namespace) {
      return namespaces.contains(namespace);
    }
  }

  public static class JsonBean {

    private String someString;
    private int someInt;

    public String getSomeString() {
      return someString;
    }

    public int getSomeInt() {
      return someInt;
    }
  }

  private static void resetApolloState() throws Exception {
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

  private static Properties copyConfigProperties(Config config) {
    Properties properties = new Properties();
    for (String key : config.getPropertyNames()) {
      properties.setProperty(key, config.getProperty(key, ""));
    }
    return properties;
  }

  private static void applyConfigChange(Config config, String appId, String namespace,
      Properties properties) {
    Assert.assertTrue(config instanceof DefaultConfig);
    ((DefaultConfig) config).onRepositoryChange(appId, namespace, properties);
  }

  private static void waitForCondition(String message, Callable<Boolean> condition) throws Exception {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
    while (System.currentTimeMillis() < deadline) {
      if (Boolean.TRUE.equals(condition.call())) {
        return;
      }
      TimeUnit.MILLISECONDS.sleep(100);
    }
    throw new AssertionError(message);
  }

}
