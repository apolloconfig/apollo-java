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
package com.ctrip.framework.apollo.spring.property;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;

/**
 * Listen and refresh ConfigurationProperties matching the configuration prefix
 *
 * @author licheng
 */
public class AutoRefreshConfigurationPropertiesListener implements
    ApplicationListener<ApolloConfigChangeEvent>,
    BeanFactoryAware {

  private static final Logger logger = LoggerFactory.getLogger(
      AutoRefreshConfigurationPropertiesListener.class);
  private static final String TEMP_PREFIX = "apollo.refresh.tmp.";

  private BeanFactory beanFactory;
  private final ConfigUtil configUtil;
  private final SpringConfigurationPropertyRegistry springConfigurationPropertyRegistry;

  public AutoRefreshConfigurationPropertiesListener() {
    this.springConfigurationPropertyRegistry = SpringInjector.getInstance(
        SpringConfigurationPropertyRegistry.class);
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public void onApplicationEvent(ApolloConfigChangeEvent event) {
    Set<String> keys = event.getConfigChangeEvent().changedKeys();
    if (!configUtil.isAutoRefreshConfigurationPropertiesEnabled() || CollectionUtils.isEmpty(
        keys)) {
      return;
    }
    // 1. check whether the changed key is relevant
    Set<String> targetBeanName = collectTargetBeanNames(keys);
    // 2. update the configuration properties
    for (String beanName : targetBeanName) {
      checkValidatorAndRefresh(beanFactory, beanName);
    }
  }

  private Set<String> collectTargetBeanNames(Set<String> keys) {
    Set<String> targetBeanNames = new HashSet<>();
    for (String key : keys) {
      Collection<String> targetCollection = springConfigurationPropertyRegistry.get(beanFactory,
          key);
      if (targetCollection != null) {
        // ensure each bean refreshed once
        targetBeanNames.addAll(targetCollection);
      }
    }
    return targetBeanNames;
  }

  private void checkValidatorAndRefresh(BeanFactory beanFactory, String beanName) {
    try {
      ConfigurationPropertiesBindingPostProcessor postProcessor = beanFactory.getBean(
          ConfigurationPropertiesBindingPostProcessor.class);
      Object bean = beanFactory.getBean(beanName);
      checkValidator(postProcessor, bean, beanName);
      refresh(postProcessor, bean, beanName);
      logger.info("Auto update apollo changed configuration properties successfully, bean: {}",
          beanName);
    } catch (Exception ex) {
      logger.error("Auto update apollo changed configuration properties failed, {}",
          beanName, ex);
    }
  }

  private void checkValidator(ConfigurationPropertiesBindingPostProcessor postProcessor,
      Object bean, String beanName)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    // `ConfigurationPropertiesBindingPostProcessor#postProcessBeforeInitialization()` can bind and validate ConfigurationProperties
    // Execution at runtime will result in an error after the bind if the verification fails
    // Using a temporary object to check validator will not affect the original bean
    postProcessor.postProcessBeforeInitialization(
        bean.getClass().getDeclaredConstructor().newInstance(), TEMP_PREFIX + beanName);
  }

  private void refresh(ConfigurationPropertiesBindingPostProcessor postProcessor, Object bean,
      String beanName) {
    postProcessor.postProcessBeforeInitialization(bean, beanName);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }
}
