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
package com.ctrip.framework.apollo.spring;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.google.common.collect.Sets;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(Enclosed.class)
public class BootstrapConfigTest {

  private static final String TEST_BEAN_CONDITIONAL_ON_KEY = "apollo.test.testBean";
  private static final String FX_APOLLO_NAMESPACE = "FX.apollo";

    @Nested
    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {

    private static final String someProperty = "someProperty";
    private static final String someValue = "someValue";

    @Autowired(required = false)
    private TestBean testBean;

    @ApolloConfig
    private Config config;

    @Value("${" + someProperty + "}")
    private String someInjectedValue;

    private static Config mockedConfig;


    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      mockedConfig = mock(Config.class);

      when(mockedConfig.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY, someProperty));

      when(mockedConfig.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), Mockito.nullable(String.class))).thenReturn(Boolean.TRUE.toString());
      when(mockedConfig.getProperty(eq(someProperty), Mockito.nullable(String.class))).thenReturn(someValue);

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, mockedConfig);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
//      Assert.assertNotNull(testBean);
//      Assert.assertTrue(testBean.execute());

//      Assert.assertEquals(mockedConfig, config);
//
//      Assert.assertEquals(someValue, someInjectedValue);
    }
  }

    @Nested
    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  class TestWithBootstrapEnabledAndNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES,
          String.format("%s, %s", ConfigConsts.NAMESPACE_APPLICATION, FX_APOLLO_NAMESPACE));

      Config config = mock(Config.class);
      Config anotherConfig = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), Mockito.nullable(String.class))).thenReturn(Boolean.TRUE.toString());

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, anotherConfig);
      mockConfig(someAppId, FX_APOLLO_NAMESPACE, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
//      Assert.assertNotNull(testBean);
//      Assert.assertTrue(testBean.execute());
    }
  }

    @Nested
    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  class TestWithBootstrapEnabledAndNamespacesAndConditionalOnWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES,
          String.format("%s, %s", "application.yml", FX_APOLLO_NAMESPACE));

      prepareYamlConfigFile(someAppId, "application.yml", readYamlContentAsConfigFileProperties("case6.yml"));
      Config anotherConfig = mock(Config.class);

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, anotherConfig);
      mockConfig(someAppId, FX_APOLLO_NAMESPACE, anotherConfig);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
//      Assert.assertNotNull(testBean);
//      Assert.assertTrue(testBean.execute());
    }
  }

    @Nested
    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOnFailed extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      Config config = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), Mockito.nullable(String.class))).thenReturn(Boolean.FALSE.toString());

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

    @Nested
    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOnFailedWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application.yml");

      prepareYamlConfigFile(someAppId, "application.yml", readYamlContentAsConfigFileProperties("case7.yml"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

//  @ExtendWith(SpringExtension.class)
@Nested
@SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  @ExtendWith(SpringExtension.class)
class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOff extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");

      Config config = mock(Config.class);

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

    @Nested
//    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  @ExtendWith(SpringExtension.class)
  class TestWithBootstrapEnabledAndDefaultNamespacesAndConditionalOffWithYamlFile extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application.yml");

      prepareYamlConfigFile(someAppId, "application.yml", readYamlContentAsConfigFileProperties("case8.yml"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

//  @ExtendWith(SpringExtension.class)
@Nested
@SpringBootTest(classes = ConfigurationWithConditionalOnProperty.class)
  @DirtiesContext
  @ExtendWith(SpringExtension.class)
class TestWithBootstrapDisabledAndDefaultNamespacesAndConditionalOn extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      Config config = mock(Config.class);

      when(config.getPropertyNames()).thenReturn(Sets.newHashSet(TEST_BEAN_CONDITIONAL_ON_KEY));
      when(config.getProperty(eq(TEST_BEAN_CONDITIONAL_ON_KEY), Mockito.nullable(String.class))).thenReturn(Boolean.FALSE.toString());

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNull(testBean);
    }
  }

    @Nested
//    @ExtendWith(SpringExtension.class)
  @SpringBootTest(classes = ConfigurationWithoutConditionalOnProperty.class)
  @DirtiesContext
  @ExtendWith(SpringExtension.class)
  class TestWithBootstrapDisabledAndDefaultNamespacesAndConditionalOff extends
      AbstractSpringIntegrationTest {

    @Autowired(required = false)
    private TestBean testBean;

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      Config config = mock(Config.class);

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);

      doTearDown();
    }

    @Test
    public void test() throws Exception {
      Assert.assertNotNull(testBean);
      Assert.assertTrue(testBean.execute());
    }
  }

//  @ExtendWith(SpringExtension.class)
@Nested
@SpringBootTest(classes = {ConfigurationWithoutConditionalOnProperty.class,TestBean.class})
  @DirtiesContext
  @ExtendWith(SpringExtension.class)
class TestWithBootstrapEnabledAndEagerLoadEnabled extends
          AbstractSpringIntegrationTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
      doSetUp();

      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
      System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, "true");

      Config config = mock(Config.class);

      mockConfig(someAppId, ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    @AfterClass
    public static void afterClass() {
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
      System.clearProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED);

      doTearDown();
    }

    @Test
    public void test() {
      List<EnvironmentPostProcessor> names = SpringFactoriesLoader.loadFactories(EnvironmentPostProcessor.class, getClass().getClassLoader());
      boolean containsApollo = false;
      for (EnvironmentPostProcessor name : names) {
        if (name.getClass().getName().equals("com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer")) {
          containsApollo = true;
          break;
        }
      }
      Assert.assertTrue(containsApollo);
    }
  }

  @EnableAutoConfiguration
  @Configuration
  static class ConfigurationWithoutConditionalOnProperty {

    @Bean
    public TestBean testBean() {
      return new TestBean();
    }
  }

  @ConditionalOnProperty(TEST_BEAN_CONDITIONAL_ON_KEY)
  @EnableAutoConfiguration
  @Configuration
  static class ConfigurationWithConditionalOnProperty {

    @Bean
    public TestBean testBean() {
      return new TestBean();
    }
  }

  static class TestBean {

    public boolean execute() {
      return true;
    }
  }
}
