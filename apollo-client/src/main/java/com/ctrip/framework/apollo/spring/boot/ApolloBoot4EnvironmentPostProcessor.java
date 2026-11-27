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
package com.ctrip.framework.apollo.spring.boot;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * Runs Apollo bootstrap in the Spring Boot 4 environment post-processor chain.
 */
public class ApolloBoot4EnvironmentPostProcessor extends ApolloBootstrapInitializer
    implements EnvironmentPostProcessor {

  /**
   * Entry point for automatic registration when Boot may also discover the legacy SPI.
   */
  public static class FactoryRegistration extends ApolloBoot4EnvironmentPostProcessor {

    private static final boolean LEGACY_PROCESSOR_PRESENT =
        ClassUtils.isPresent("org.springframework.boot.env.EnvironmentPostProcessor",
            FactoryRegistration.class.getClassLoader());

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
        SpringApplication application) {
      // Boot 4.0 and 4.1 invoke Apollo through the legacy registration in the same chain.
      if (!LEGACY_PROCESSOR_PRESENT) {
        super.postProcessEnvironment(environment, application);
      }
    }
  }
}
