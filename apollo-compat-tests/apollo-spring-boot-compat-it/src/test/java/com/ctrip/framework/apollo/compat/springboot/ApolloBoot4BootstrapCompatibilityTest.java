/*
 * Copyright 2026 Apollo Authors
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
import static org.junit.Assume.assumeTrue;

import com.ctrip.framework.apollo.mockserver.ApolloTestingServer;
import com.ctrip.framework.apollo.spring.boot.ApolloBoot4EnvironmentPostProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

public class ApolloBoot4BootstrapCompatibilityTest {

  private static final String ENABLE_PROBE = "apollo.compat.bootstrap-probe.enabled";
  private static final String APOLLO_VALUE = "apollo.compat.bootstrap-only-value";
  private static final String OBSERVED_VALUE = "apollo.compat.bootstrap-probe.value";
  private static final String MODERN_PROCESSOR =
      "org.springframework.boot.EnvironmentPostProcessor";

  @Test
  public void laterProcessorSeesApolloValueAndBootstrapRunsOnce() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    assumeTrue(ClassUtils.isPresent(MODERN_PROCESSOR, classLoader));
    String processorResource = MODERN_PROCESSOR.replace('.', '/') + ".class";
    assertEquals(1, Collections.list(classLoader.getResources(processorResource)).size());

    Properties originalProperties = (Properties) System.getProperties().clone();
    System.setProperty("app.id", "someAppId");
    System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application");
    System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
    System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, "true");
    try (ApolloTestingServer server = new ApolloTestingServer()) {
      server.start();
      server.addOrModifyProperty("application", APOLLO_VALUE, "from-apollo");
      CountingEnvironment environment = new CountingEnvironment();
      assertBootstrap(application(environment), environment);

      CountingEnvironment explicitEnvironment = new CountingEnvironment();
      SpringApplication explicitApplication = application(explicitEnvironment);
      ApplicationListener<?> explicitListener = explicitListener(classLoader);
      List<ApplicationListener<?>> listeners = new ArrayList<>(explicitApplication.getListeners());
      listeners.removeIf(listener -> listener.getClass() == explicitListener.getClass());
      listeners.add(explicitListener);
      explicitApplication.setListeners(listeners);
      assertBootstrap(explicitApplication, explicitEnvironment);
    } finally {
      System.setProperties(originalProperties);
    }
  }

  private SpringApplication application(CountingEnvironment environment) {
    SpringApplication application = new SpringApplication(TestConfiguration.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.setDefaultProperties(Collections.singletonMap(ENABLE_PROBE, true));
    application.setEnvironment(environment);
    return application;
  }

  private void assertBootstrap(SpringApplication application, CountingEnvironment environment) {
    try (ConfigurableApplicationContext context = application.run()) {
      assertEquals("from-apollo", context.getEnvironment().getProperty(OBSERVED_VALUE));
      assertEquals(1, environment.bootstrapReads);
    }
  }

  private ApplicationListener<?> explicitListener(ClassLoader classLoader)
      throws ReflectiveOperationException {
    // The factory API is only available on Boot 4, but this suite also compiles on Boot 2 and 3.
    Class<?> factory =
        classLoader.loadClass("org.springframework.boot.support.EnvironmentPostProcessorsFactory");
    Class<?> listener = classLoader
        .loadClass("org.springframework.boot.support.EnvironmentPostProcessorApplicationListener");
    Object processors = factory.getMethod("of", Class[].class).invoke(null,
        (Object) new Class<?>[] {ApolloBoot4EnvironmentPostProcessor.class, LaterProcessor.class});
    return (ApplicationListener<?>) listener.getMethod("with", factory).invoke(null, processors);
  }

  @Configuration
  static class TestConfiguration {
  }

  private static class CountingEnvironment extends StandardEnvironment {

    private int bootstrapReads;

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
      if (PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED.equals(key)) {
        bootstrapReads++;
      }
      return super.getProperty(key, targetType, defaultValue);
    }
  }

  public static class LaterProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
      return 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
        SpringApplication application) {
      if (environment.getProperty(ENABLE_PROBE, Boolean.class, false)) {
        String value = environment.getProperty(APOLLO_VALUE, "missing");
        environment.getPropertySources().addFirst(new MapPropertySource("apolloBootstrapProbe",
            Collections.singletonMap(OBSERVED_VALUE, value)));
      }
    }
  }
}
