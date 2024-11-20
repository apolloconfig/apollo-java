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
package com.ctrip.framework.apollo.spring.spi;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.spring.annotation.ApolloAnnotationProcessor;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.SpringValueProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.property.AutoUpdateConfigChangeListener;
import com.ctrip.framework.apollo.spring.property.SpringValueDefinitionProcessor;
import com.ctrip.framework.apollo.spring.util.BeanRegistrationUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

public class DefaultApolloConfigRegistrarHelper implements ApolloConfigRegistrarHelper {
  private static final Logger logger = LoggerFactory.getLogger(
      DefaultApolloConfigRegistrarHelper.class);

  private final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  private Environment environment;

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AnnotationAttributes attributes = AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(EnableApolloConfig.class.getName()));
    final String[] namespaces = attributes.getStringArray("value");
    final int order = attributes.getNumber("order");

    // put main appId
    PropertySourcesProcessor.addNamespaces(configUtil.getAppId(), Lists.newArrayList(this.resolveNamespaces(namespaces)), order);

    // put multiple appId into
    AnnotationAttributes[] multipleConfigs = attributes.getAnnotationArray("multipleConfigs");
    if (multipleConfigs != null) {
      for (AnnotationAttributes multipleConfig : multipleConfigs) {
        String appId = multipleConfig.getString("appId");
        String[] multipleNamespaces = this.resolveNamespaces(multipleConfig.getStringArray("namespaces"));
        String secret = resolveSecret(multipleConfig.getString("secret"));
        int multipleOrder = multipleConfig.getNumber("order");

        // put multiple secret into system property
        System.setProperty("apollo.accesskey." + appId + ".secret", secret);
        PropertySourcesProcessor.addNamespaces(appId, Lists.newArrayList(multipleNamespaces), multipleOrder);
      }
    }

    Map<String, Object> propertySourcesPlaceholderPropertyValues = new HashMap<>();
    // to make sure the default PropertySourcesPlaceholderConfigurer's priority is higher than PropertyPlaceholderConfigurer
    propertySourcesPlaceholderPropertyValues.put("order", 0);

    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesPlaceholderConfigurer.class,
            propertySourcesPlaceholderPropertyValues);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, AutoUpdateConfigChangeListener.class);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesProcessor.class);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, ApolloAnnotationProcessor.class);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueProcessor.class);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueDefinitionProcessor.class);
  }

  private String[] resolveNamespaces(String[] namespaces) {
    // no support for Spring version prior to 3.2.x, see https://github.com/apolloconfig/apollo/issues/4178
    if (this.environment == null) {
      logNamespacePlaceholderNotSupportedMessage(namespaces);
      return namespaces;
    }
    String[] resolvedNamespaces = new String[namespaces.length];
    for (int i = 0; i < namespaces.length; i++) {
      // throw IllegalArgumentException if given text is null or if any placeholders are unresolvable
      resolvedNamespaces[i] = this.environment.resolveRequiredPlaceholders(namespaces[i]);
    }
    return resolvedNamespaces;
  }

  private String resolveSecret(String secret){
    if (this.environment == null) {
      if (secret.contains("${")) {
        logger.warn("secret placeholder {} is not supported for Spring version prior to 3.2.x", secret);
      }
      return secret;
    }
    return this.environment.resolveRequiredPlaceholders(secret);
  }

  private void logNamespacePlaceholderNotSupportedMessage(String[] namespaces) {
    for (String namespace : namespaces) {
      if (namespace.contains("${")) {
        logger.warn("Namespace placeholder {} is not supported for Spring version prior to 3.2.x,"
                + " see https://github.com/apolloconfig/apollo/issues/4178 for more details.",
            namespace);
        break;
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
