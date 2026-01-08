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
package com.ctrip.framework.apollo.mockserver;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.rules.ExternalResource;

/**
 * Create by zhangzheng on 8/22/18 Email:zhangzheng@youzan.com
 */
public class EmbeddedApollo implements BeforeAllCallback, AfterAllCallback , ParameterResolver {

  private final ApolloTestingServer apollo = new ApolloTestingServer();

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(EmbeddedApollo.class);


    /**
   * Add new property or update existed property
   */
  public void addOrModifyProperty(String namespace, String someKey, String someValue) {
    apollo.addOrModifyProperty(namespace, someKey, someValue);
  }

  /**
   * Delete existed property
   */
  public void deleteProperty(String namespace, String someKey) {
    apollo.deleteProperty(namespace, someKey);
  }

  /**
   * reset overridden properties
   */
  public void resetOverriddenProperties() {
    apollo.resetOverriddenProperties();
  }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        EmbeddedApollo embeddedApollo =
            context.getStore(NAMESPACE).remove(EmbeddedApollo.class, EmbeddedApollo.class);
        if (embeddedApollo != null) {
            embeddedApollo.apollo.close();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        apollo.start();
        context.getStore(NAMESPACE).put(EmbeddedApollo.class, this);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == EmbeddedApollo.class;
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext
            .getStore(NAMESPACE)
            .get(EmbeddedApollo.class, EmbeddedApollo.class);
    }
}
