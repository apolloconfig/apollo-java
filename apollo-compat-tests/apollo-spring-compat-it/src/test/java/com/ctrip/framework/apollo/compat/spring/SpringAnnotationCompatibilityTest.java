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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.MultipleConfig;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringAnnotationCompatibilityTest.TestConfiguration.class)
public class SpringAnnotationCompatibilityTest {

  private static final String ANOTHER_APP_ID = "100004459";

  @ClassRule
  public static final EmbeddedApollo EMBEDDED_APOLLO = new EmbeddedApollo();

  @Autowired
  private AnnotationProbe probe;

  @Autowired
  private SpringApolloEventListenerProbe apolloEventListenerProbe;

  @BeforeClass
  public static void beforeClass() throws Exception {
    SpringCompatibilityTestSupport.beforeClass(EMBEDDED_APOLLO);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    SpringCompatibilityTestSupport.afterClass();
  }

  @Test
  public void shouldSupportAnnotationAndMultipleConfig() throws Exception {
    assertEquals(5001, probe.getTimeout());
    assertEquals("from-public", probe.getPublicValue());
    assertEquals("from-yaml", probe.getYamlMarker());
    assertEquals("from-another-app", probe.getAnotherAppConfig().getProperty("compat.origin", null));
    assertEquals("5001", probe.getApplicationConfig().getProperty("compat.timeout", null));
    assertEquals("from-public", probe.getPublicNamespaceConfig().getProperty("public.key", null));
    assertEquals("from-yaml", probe.getYamlNamespaceConfig().getProperty("yaml.marker", null));
    assertEquals(2, probe.getJsonBeans().size());
    assertEquals("alpha", probe.getJsonBeans().get(0).getSomeString());

    Config applicationConfig = ConfigService.getConfig("application");
    Properties applicationProperties =
        SpringCompatibilityTestSupport.copyConfigProperties(applicationConfig);
    applicationProperties.setProperty("compat.timeout", "5002");
    SpringCompatibilityTestSupport.applyConfigChange(applicationConfig, "application",
        applicationProperties);

    Config publicConfig = ConfigService.getConfig("TEST1.apollo");
    Properties publicProperties = SpringCompatibilityTestSupport.copyConfigProperties(publicConfig);
    publicProperties.setProperty("public.key", "from-public-updated");
    SpringCompatibilityTestSupport.applyConfigChange(publicConfig, "TEST1.apollo", publicProperties);

    Config yamlConfig = ConfigService.getConfig("application.yaml");
    Properties yamlProperties = SpringCompatibilityTestSupport.copyConfigProperties(yamlConfig);
    yamlProperties.setProperty("yaml.marker", "from-yaml-updated");
    SpringCompatibilityTestSupport.applyConfigChange(yamlConfig, "application.yaml", yamlProperties);

    Properties anotherAppProperties =
        SpringCompatibilityTestSupport.copyConfigProperties(probe.getAnotherAppConfig());
    anotherAppProperties.setProperty("compat.origin", "changed-origin");
    SpringCompatibilityTestSupport.applyConfigChange(probe.getAnotherAppConfig(), ANOTHER_APP_ID,
        "application", anotherAppProperties);

    ConfigChangeEvent defaultChange = probe.pollDefaultEvent(10, TimeUnit.SECONDS);
    assertNotNull(defaultChange);
    assertNotNull(defaultChange.getChange("compat.timeout"));

    ConfigChangeEvent publicNamespaceChange = probe.pollPublicNamespaceEvent(10, TimeUnit.SECONDS);
    assertNotNull(publicNamespaceChange);
    assertNotNull(publicNamespaceChange.getChange("public.key"));

    ConfigChangeEvent yamlNamespaceChange = probe.pollYamlNamespaceEvent(10, TimeUnit.SECONDS);
    assertNotNull(yamlNamespaceChange);
    assertEquals("application.yaml", yamlNamespaceChange.getNamespace());

    ConfigChangeEvent anotherAppChange = probe.pollAnotherAppEvent(10, TimeUnit.SECONDS);
    assertNotNull(anotherAppChange);
    assertNotNull(anotherAppChange.getChange("compat.origin"));

    String namespace = apolloEventListenerProbe.pollNamespace(10, TimeUnit.SECONDS);
    assertEquals("application", namespace);

    SpringCompatibilityTestSupport.waitForCondition("public value should be updated",
        () -> "from-public-updated".equals(
            probe.getPublicNamespaceConfig().getProperty("public.key", null)));
    SpringCompatibilityTestSupport.waitForCondition("yaml marker should be updated",
        () -> "from-yaml-updated".equals(
            probe.getYamlNamespaceConfig().getProperty("yaml.marker", null)));
    SpringCompatibilityTestSupport.waitForCondition("application config should be updated",
        () -> "5002".equals(probe.getApplicationConfig().getProperty("compat.timeout", null)));
    SpringCompatibilityTestSupport.waitForCondition("another app config should be updated",
        () -> "changed-origin".equals(probe.getAnotherAppConfig().getProperty("compat.origin", null)));
  }

  @Configuration
  @EnableApolloConfig(value = {"application", "TEST1.apollo", "application.yaml"},
      multipleConfigs = {@MultipleConfig(appId = ANOTHER_APP_ID, namespaces = {"application"}, order = 9)})
  static class TestConfiguration {

    @Bean
    public AnnotationProbe annotationProbe() {
      return new AnnotationProbe();
    }

    @Bean
    public SpringApolloEventListenerProbe apolloEventListenerProbe() {
      return new SpringApolloEventListenerProbe();
    }
  }

  static class AnnotationProbe {

    private final BlockingQueue<ConfigChangeEvent> defaultEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> publicNamespaceEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> yamlNamespaceEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);
    private final BlockingQueue<ConfigChangeEvent> anotherAppEvents =
        new ArrayBlockingQueue<ConfigChangeEvent>(8);

    private volatile int timeout;
    private volatile String publicValue;
    private volatile String yamlMarker;
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

    @Value("${public.key:missing}")
    public void setPublicValue(String publicValue) {
      this.publicValue = publicValue;
    }

    @Value("${yaml.marker:missing}")
    public void setYamlMarker(String yamlMarker) {
      this.yamlMarker = yamlMarker;
    }

    @ApolloJsonValue("${jsonBeanProperty:[]}")
    public void setJsonBeans(List<JsonBean> jsonBeans) {
      this.jsonBeans = jsonBeans;
    }

    @ApolloConfigChangeListener
    private void onDefaultNamespaceChange(ConfigChangeEvent event) {
      defaultEvents.offer(event);
    }

    @ApolloConfigChangeListener("TEST1.apollo")
    private void onPublicNamespaceChange(ConfigChangeEvent event) {
      publicNamespaceEvents.offer(event);
    }

    @ApolloConfigChangeListener("application.yaml")
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

    String getPublicValue() {
      return publicValue;
    }

    String getYamlMarker() {
      return yamlMarker;
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

  static class JsonBean {

    private String someString;
    private int someInt;

    public String getSomeString() {
      return someString;
    }

    public int getSomeInt() {
      return someInt;
    }
  }
}
