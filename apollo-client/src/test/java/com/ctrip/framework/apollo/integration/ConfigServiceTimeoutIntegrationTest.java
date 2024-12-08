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
package com.ctrip.framework.apollo.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.BaseIntegrationTest;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.MockedConfigService;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.util.OrderedProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class ConfigServiceTimeoutIntegrationTest extends BaseIntegrationTest {

  @Test
  void timeoutWhenGet10Namespace() throws ExecutionException, InterruptedException, TimeoutException {
    MockedConfigService  mockedConfigService = newMockedConfigService();
    mockedConfigService.mockMetaSeverWithDelay(30_000);

    final SettableFuture<Boolean> loadFinished = SettableFuture.create();

    final int namespaceCount = 10;
    List<Config> configList = new ArrayList<>(namespaceCount);
    Runnable runnable = () -> {
      // try to get multiple namespace
      for (int i = 0; i < namespaceCount; i++) {
        String ns = "ns" + i;
        Config config = ConfigService.getConfig(ns);
        configList.add(config);
      }
      loadFinished.set(true);
    };
    Thread thread = new Thread(runnable);
    thread.start();

    loadFinished.get(10_000, TimeUnit.MILLISECONDS);
    assertEquals(namespaceCount, configList.size());
  }

  @Test
  void giveDefaultNamespaceConfigBackupWhenMeetTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    Properties properties = new OrderedProperties();
    properties.put("k1", "v1");
    properties.put("k2", "v2");
    properties.put("k3", "v3");
    createLocalCachePropertyFile(properties);

    MockedConfigService  mockedConfigService = newMockedConfigService();
    mockedConfigService.mockMetaSeverWithDelay(30_000);

    final SettableFuture<Config> configSettableFuture = SettableFuture.create();
    Runnable runnable = () -> {
      Config config = ConfigService.getAppConfig();
      configSettableFuture.set(config);
    };
    Thread thread = new Thread(runnable);
    thread.start();

    Config config = configSettableFuture.get(10_000, TimeUnit.MILLISECONDS);
    assertEquals("v1", config.getProperty("k1", null));
    assertEquals("v2", config.getProperty("k2", null));
    assertEquals("v3", config.getProperty("k3", null));
  }

  @Test
  void give9NamespaceConfigBackupWhenMeetTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    final int numberOfNamespace = 10;
    final String nsPrefix = "backupNs";
    List<String> nsList = new ArrayList<>(numberOfNamespace);
    List<SettableFuture<Config>> futureList = new ArrayList<>(numberOfNamespace);

    // init
    for (int i = 0; i < numberOfNamespace; i++) {
      final String ns = nsPrefix + i;
      nsList.add(ns);
      futureList.add(SettableFuture.create());
    }

    // create backup for 9 namespace
    // there is no backup for last namespace
    for (int i = 0; i < numberOfNamespace - 1; i++) {
      final String ns = nsPrefix + i;
      // create backup in local
      Properties properties = new OrderedProperties();
      properties.put(ns + ".k1", ns + ".v1");
      properties.put(ns + ".k2", ns + ".v2");
      properties.put(ns + ".k3", ns + ".v3");
      createLocalCachePropertyFile(ns, properties);
    }

    MockedConfigService  mockedConfigService = newMockedConfigService();
    mockedConfigService.mockMetaSeverWithDelay(30_000);

    // concurrent execute
    for (int i = 0; i < numberOfNamespace; i++) {
      final String ns = nsList.get(i);
      final int index = i;
      Runnable runnable = () -> {
          Config config = ConfigService.getConfig(ns);
          futureList.get(index).set(config);
      };
      Thread thread = new Thread(runnable);
      thread.start();
    }

    // wait last namespace
    {
      SettableFuture<Config> future = futureList.get(numberOfNamespace - 1);
      Config config = future.get(10_000, TimeUnit.MILLISECONDS);
      // no item here because there is no backup for this namespace
      assertTrue(config.getPropertyNames().isEmpty());
    }

    // check 9 namespace
    for (int i = 0; i < numberOfNamespace - 1; i++) {
      String ns = nsList.get(i);
      SettableFuture<Config> future = futureList.get(i);
      Config config = future.get(10_000, TimeUnit.MILLISECONDS);
      assertEquals(ns + ".v1", config.getProperty(ns + ".k1", null));
      assertEquals(ns + ".v2", config.getProperty(ns + ".k2", null));
      assertEquals(ns + ".v3", config.getProperty(ns + ".k3", null));
    }

  }

  @Test
  void giveTimeoutThenRecover() throws ExecutionException, InterruptedException, TimeoutException {
    final String ns = "recoverNsXxx";
    final String k1 = "k1";
    final String k2 = "k2";
    final String k3 = "k3";

    final String v1 = "v1";
    final String v2 = "v2";
    final String v3 = "v3";

    // create backup first
    {
      Properties properties = new OrderedProperties();
      properties.put(k1, v1);
      properties.put(k2, v2);
      properties.put(k3, v3);
      createLocalCachePropertyFile(ns, properties);
    }

    MockedConfigService  mockedConfigService = newMockedConfigService();

    // simulate timeout, so will read local backup
    mockedConfigService.mockMetaSeverWithDelay(30_000);
    {
      final SettableFuture<Config> configSettableFuture = SettableFuture.create();
      Runnable runnable = () -> {
        Config config = ConfigService.getConfig(ns);
        configSettableFuture.set(config);
      };
      Thread thread = new Thread(runnable);
      thread.start();

      Config config = configSettableFuture.get(10_000, TimeUnit.MILLISECONDS);
      log.info("get namespace '{}' from backup success", ns);
      assertEquals(v1, config.getProperty(k1, null));
      assertEquals(v2, config.getProperty(k2, null));
      assertEquals(v3, config.getProperty(k3, null));
    }

    final BlockingQueue<ConfigChangeEvent> longPollFinishedSignal = new ArrayBlockingQueue<>(1);
    // add listener first
    final Config config = ConfigService.getConfig(ns);
    config.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        longPollFinishedSignal.add(changeEvent);
      }
    });

    // recover and change the config
    final String recoverValue = "recoverValue";
    final long recoverNotificationId = 1;
    final long pollTimeoutInMS = 50;

    // change the config and notify
    {
      Map<String, String> configurations = Maps.newHashMap();
      configurations.put(k1, recoverValue);
      ApolloConfig apolloConfig = assembleApolloConfig(ns, "releaseKey1", configurations);
      // change the config
      mockConfigs(HttpServletResponse.SC_OK, apolloConfig);
      // notify
      mockedConfigService.mockLongPollNotifications(
          pollTimeoutInMS, HttpServletResponse.SC_OK,
          Lists.newArrayList(
              new ApolloConfigNotification(apolloConfig.getNamespaceName(), recoverNotificationId))
      );
    }

    // recover meta service
    log.info("recover meta service");
    mockMetaServer();

    // wait
    {
      ConfigChangeEvent event = longPollFinishedSignal.poll(10_000, TimeUnit.MILLISECONDS);
      assertNotNull(event, "namespace '" + ns + "' should receive notify");
    }

    // check config
    assertEquals(recoverValue, config.getProperty(k1, null));
    assertNull(config.getProperty(k2, null));
    assertNull(config.getProperty(k3, null));

    log.info("change the config and notify again");
    final String anotherRecoverValue = "anotherRecoverValue";
    final long anotherRecoverNotificationId = 2;
    {
      Map<String, String> configurations = Maps.newHashMap();
      configurations.put(k1, anotherRecoverValue);
      ApolloConfig apolloConfig = assembleApolloConfig(ns, "releaseKey2", configurations);
      // change the config
      mockConfigs(HttpServletResponse.SC_OK, apolloConfig);
      // notify
      mockedConfigService.mockLongPollNotifications(
          pollTimeoutInMS, HttpServletResponse.SC_OK,
          Lists.newArrayList(
              new ApolloConfigNotification(apolloConfig.getNamespaceName(), anotherRecoverNotificationId))
      );
    }

    // wait
    longPollFinishedSignal.poll(5000, TimeUnit.MILLISECONDS);
    // check config
    assertEquals(anotherRecoverValue, config.getProperty(k1, null));
    assertNull(config.getProperty(k2, null));
    assertNull(config.getProperty(k3, null));
  }
}
