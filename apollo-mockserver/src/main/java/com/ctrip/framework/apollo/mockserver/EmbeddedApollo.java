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

import org.junit.rules.ExternalResource;

/**
 * Create by zhangzheng on 8/22/18 Email:zhangzheng@youzan.com
 */
public class EmbeddedApollo extends ExternalResource {

  private ApolloTestingServer apollo = new ApolloTestingServer();

  @Override
  protected void before() throws Throwable {
    apollo.start();
    super.before();
  }

  @Override
  protected void after() {
    apollo.close();
  }

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
}
