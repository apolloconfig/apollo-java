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

import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;

/**
 * @author licheng
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public class AutoRefreshConfigurationPropertiesListener implements
    ApplicationListener<ApolloConfigChangeEvent>,
    ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(
      AutoRefreshConfigurationPropertiesListener.class);

  private AutowireCapableBeanFactory beanFactory;
  private boolean supportAutowireCapableBeanFactory = false;
  private final SpringConfigurationPropertyRegistry springConfigurationPropertyRegistry;

  public AutoRefreshConfigurationPropertiesListener() {
    this.springConfigurationPropertyRegistry = SpringInjector.getInstance(
        SpringConfigurationPropertyRegistry.class);
  }


  @Override
  public void onApplicationEvent(ApolloConfigChangeEvent event) {
    if (!supportAutowireCapableBeanFactory) {
      return;
    }
    Set<String> keys = event.getConfigChangeEvent().changedKeys();
    if (CollectionUtils.isEmpty(keys)) {
      return;
    }
    Set<String> targetBeanName = new HashSet<>();
    for (String key : keys) {
      // 1. check whether the changed key is relevant
      Collection<String> targetCollection = springConfigurationPropertyRegistry.get(beanFactory,
          key);
      if (targetCollection != null) {
        // ensure each bean refreshed once
        targetBeanName.addAll(targetCollection);
      }
    }
    // 2. update the configuration properties
    for (String beanName : targetBeanName) {
      refreshConfigurationProperties(beanFactory, beanName);
    }
  }

  private void refreshConfigurationProperties(AutowireCapableBeanFactory beanFactory,
      String beanName) {
    try {
      springConfigurationPropertyRegistry.refresh(beanFactory, beanName);
      logger.info("Auto update apollo changed configuration properties successfully, bean: {}",
          beanName);
    } catch (Throwable ex) {
      logger.error("Auto update apollo changed configuration properties failed, {}",
          beanName, ex);
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    try {
      this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
      this.supportAutowireCapableBeanFactory = true;
    } catch (IllegalStateException e) {
      logger.warn(
          "Failed to init AutoRefreshConfigurationPropertiesListener", e);
    }
  }
}
