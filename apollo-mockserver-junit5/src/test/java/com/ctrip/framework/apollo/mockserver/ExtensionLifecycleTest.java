/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockApolloExtension.class)
public class ExtensionLifecycleTest {

  private ApolloTestingServer target;

  @AfterEach
  public void after() {
    Assertions.assertTrue(target.isClosed());
  }

  @Test
  public void testParameterInjection(ApolloTestingServer server) throws Exception {
    Assertions.assertTrue(server.isStarted());
    target = server;
    Config applicationConfig = ConfigService.getAppConfig();

    CompletableFuture<ConfigChangeEvent> future = new CompletableFuture<>();
    applicationConfig.addChangeListener(future::complete);

    assertEquals("value1", applicationConfig.getProperty("key1", null));
    assertEquals("value2", applicationConfig.getProperty("key2", null));

    server.addOrModifyProperty(ConfigConsts.NAMESPACE_APPLICATION, "key2", "newValue2");
    future.get(5, TimeUnit.SECONDS);
    assertEquals("newValue2", applicationConfig.getProperty("key2", null));
  }
}
