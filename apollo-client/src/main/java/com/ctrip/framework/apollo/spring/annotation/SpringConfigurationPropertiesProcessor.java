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
package com.ctrip.framework.apollo.spring.annotation;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.property.SpringConfigurationPropertyRegistry;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import java.lang.annotation.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Configuration properties processor
 *
 * @author licheng
 */
public class SpringConfigurationPropertiesProcessor implements BeanPostProcessor,
    ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(
      SpringConfigurationPropertiesProcessor.class);
  private static final String REFRESH_SCOPE_NAME = "org.springframework.cloud.context.config.annotation.RefreshScope";

  private final ConfigUtil configUtil;
  private final SpringConfigurationPropertyRegistry springConfigurationPropertyRegistry;
  private AutowireCapableBeanFactory beanFactory;
  private boolean supportAutowireCapableBeanFactory = false;

  public SpringConfigurationPropertiesProcessor() {
    springConfigurationPropertyRegistry = SpringInjector.getInstance(
        SpringConfigurationPropertyRegistry.class);
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    if (!supportAutowireCapableBeanFactory
        || !configUtil.isAutoRefreshConfigurationPropertiesEnabled()) {
      return bean;
    }
    Class<?> clazz = bean.getClass();
    ConfigurationProperties configurationPropertiesAnnotation = clazz.getDeclaredAnnotation(
        ConfigurationProperties.class);
    // match beans with annotated `@ConfigurationProperties` and `@ApolloConfigurationPropertiesRefresh`,
    // or `@ConfigurationProperties` and `@RefreshScope`
    if (configurationPropertiesAnnotation != null && annotatedRefresh(clazz)) {
      String prefix = configurationPropertiesAnnotation.prefix();
      // cache prefix and bean name
      springConfigurationPropertyRegistry.register(this.beanFactory, prefix, beanName);
      logger.debug("Monitoring bean {}", beanName);
    }
    return bean;
  }

  private boolean annotatedRefresh(Class<?> clazz) {
    ApolloConfigurationPropertiesRefresh apolloConfigurationPropertiesRefreshAnnotation = clazz.getDeclaredAnnotation(
        ApolloConfigurationPropertiesRefresh.class);
    return apolloConfigurationPropertiesRefreshAnnotation != null || isRefreshScope(
        clazz.getDeclaredAnnotations());
  }

  private boolean isRefreshScope(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getName().equals(REFRESH_SCOPE_NAME)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    try {
      this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
      this.supportAutowireCapableBeanFactory = true;
    } catch (IllegalStateException e) {
      logger.warn("Failed to initialize SpringConfigurationPropertiesProcessor, message:{}",
          ExceptionUtil.getDetailMessage(e));
    }
  }
}
