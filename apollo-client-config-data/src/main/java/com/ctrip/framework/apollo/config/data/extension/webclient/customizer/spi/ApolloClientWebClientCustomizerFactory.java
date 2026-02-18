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
package com.ctrip.framework.apollo.config.data.extension.webclient.customizer.spi;

import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import com.ctrip.framework.apollo.core.spi.Ordered;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public interface ApolloClientWebClientCustomizerFactory extends Ordered {

  /**
   * create a webclient builder customizer
   *
   * @param apolloClientProperties apollo client binded properties
   * @param binder                 properties binder
   * @param bindHandler            properties binder Handler
   * @param log                    deferred log
   * @param bootstrapContext       bootstrapContext (can be either
   *                               org.springframework.boot.ConfigurableBootstrapContext for
   *                               Spring Boot 3.x or
   *                               org.springframework.boot.bootstrap.ConfigurableBootstrapContext
   *                               for Spring Boot 4.x)
   * @return customizer instance or null
   */
  @Nullable
  default Consumer<WebClient.Builder> createWebClientCustomizer(
      ApolloClientProperties apolloClientProperties,
      Binder binder,
      BindHandler bindHandler,
      Log log,
      Object bootstrapContext) {
    // Keep compatibility for third-party extensions compiled with the old
    // createWebClientCustomizer(...):WebClientCustomizer signature.
    Method legacyMethod = findLegacyCustomizerMethod(this.getClass());
    if (legacyMethod == null) {
      return null;
    }
    try {
      Object customizer = legacyMethod
          .invoke(this, apolloClientProperties, binder, bindHandler, log, bootstrapContext);
      return asBuilderConsumer(customizer);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to invoke legacy createWebClientCustomizer method", ex);
    }
  }

  static Method findLegacyCustomizerMethod(Class<?> implementationType) {
    for (Method method : implementationType.getMethods()) {
      if (!"createWebClientCustomizer".equals(method.getName())
          || method.getDeclaringClass() == ApolloClientWebClientCustomizerFactory.class) {
        continue;
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 5
          && ApolloClientProperties.class == parameterTypes[0]
          && Binder.class == parameterTypes[1]
          && BindHandler.class == parameterTypes[2]
          && Log.class == parameterTypes[3]
          && Object.class == parameterTypes[4]) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  static Consumer<WebClient.Builder> asBuilderConsumer(Object customizer) {
    if (customizer == null) {
      return null;
    }
    if (customizer instanceof Consumer) {
      return (Consumer<WebClient.Builder>) customizer;
    }
    try {
      Method customizeMethod = customizer.getClass().getMethod("customize", WebClient.Builder.class);
      customizeMethod.setAccessible(true);
      return builder -> {
        try {
          customizeMethod.invoke(customizer, builder);
        } catch (Exception ex) {
          throw new IllegalStateException("Failed to invoke legacy WebClient customizer", ex);
        }
      };
    } catch (NoSuchMethodException ex) {
      throw new IllegalStateException(
          "Unsupported customizer type from createWebClientCustomizer: " + customizer.getClass(), ex);
    }
  }
}
