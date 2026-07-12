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
package com.ctrip.framework.apollo.config.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.junit.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientConfigDataAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ApolloClientConfigDataAutoConfiguration.class));

  @Test
  public void testDefaultBeansLoaded() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(ApolloClientProperties.class);
      assertThat(context).hasSingleBean(PropertySourcesProcessor.class);
      assertThat(context.getBean(PropertySourcesProcessor.class))
          .isInstanceOf(ConfigPropertySourcesProcessor.class);
    });
  }

  @Test
  public void testConditionalOnMissingBean() {
    contextRunner.withUserConfiguration(CustomBeansConfiguration.class).run(context -> {
      assertThat(context).hasSingleBean(ApolloClientProperties.class);
      assertThat(context.getBean(ApolloClientProperties.class))
          .isSameAs(context.getBean("customApolloClientProperties"));
      assertThat(context.getBean(PropertySourcesProcessor.class))
          .isSameAs(context.getBean("customPropertySourcesProcessor"));
    });
  }

  @Configuration
  static class CustomBeansConfiguration {

    @Bean
    public ApolloClientProperties customApolloClientProperties() {
      return new ApolloClientProperties();
    }

    @Bean
    public static PropertySourcesProcessor customPropertySourcesProcessor() {
      return new ConfigPropertySourcesProcessor();
    }
  }
}
