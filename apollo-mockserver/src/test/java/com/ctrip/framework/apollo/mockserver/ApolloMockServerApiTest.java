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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EmbeddedApollo.class)
public class ApolloMockServerApiTest {

  private static final String anotherNamespace = "anotherNamespace";

  @Test
  public void testGetProperty() throws Exception {
    Config applicationConfig = ConfigService.getAppConfig();

    assertEquals("value1", applicationConfig.getProperty("key1", null));
    assertEquals("value2", applicationConfig.getProperty("key2", null));
  }

  @Test
  public void testUpdateProperties(EmbeddedApollo embeddedApollo) throws Exception {
    String someNewValue = "someNewValue";

    Config otherConfig = ConfigService.getConfig(anotherNamespace);

    final SettableFuture<ConfigChangeEvent> future = SettableFuture.create();

    otherConfig.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        future.set(changeEvent);
      }
    });

    assertEquals("otherValue1", otherConfig.getProperty("key1", null));
    assertEquals("otherValue2", otherConfig.getProperty("key2", null));

    embeddedApollo.addOrModifyProperty(anotherNamespace, "key1", someNewValue);

    ConfigChangeEvent changeEvent = future.get(5, TimeUnit.SECONDS);

    assertEquals(someNewValue, otherConfig.getProperty("key1", null));
    assertEquals("otherValue2", otherConfig.getProperty("key2", null));
    assertTrue(changeEvent.isChanged("key1"));
  }

  @Test
  public void testUpdateSamePropertyTwice(EmbeddedApollo embeddedApollo) throws Exception {
    String someNewValue = "someNewValue";

    Config otherConfig = ConfigService.getConfig(anotherNamespace);

    final Semaphore changes = new Semaphore(0);

    otherConfig.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        changes.release();
      }
    });

    assertEquals("otherValue3", otherConfig.getProperty("key3", null));

    embeddedApollo.addOrModifyProperty(anotherNamespace, "key3", someNewValue);
    embeddedApollo.addOrModifyProperty(anotherNamespace, "key3", someNewValue);

    assertTrue(changes.tryAcquire(5, TimeUnit.SECONDS));
    assertEquals(someNewValue, otherConfig.getProperty("key3", null));
    assertEquals(0, changes.availablePermits());
  }

  @Test
  public void testDeleteProperties(EmbeddedApollo embeddedApollo) throws Exception {
    Config otherConfig = ConfigService.getConfig(anotherNamespace);

    final SettableFuture<ConfigChangeEvent> future = SettableFuture.create();

    otherConfig.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        future.set(changeEvent);
      }
    });

    assertEquals("otherValue4", otherConfig.getProperty("key4", null));
    assertEquals("otherValue5", otherConfig.getProperty("key5", null));

    embeddedApollo.deleteProperty(anotherNamespace, "key4");

    ConfigChangeEvent changeEvent = future.get(5, TimeUnit.SECONDS);

    assertNull(otherConfig.getProperty("key4", null));
    assertEquals("otherValue5", otherConfig.getProperty("key5", null));
    assertTrue(changeEvent.isChanged("key4"));
  }

  @Test
  public void testDeleteSamePropertyTwice(EmbeddedApollo embeddedApollo) throws Exception {
    Config otherConfig = ConfigService.getConfig(anotherNamespace);

    final Semaphore changes = new Semaphore(0);

    otherConfig.addChangeListener(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        changes.release();
      }
    });

    assertEquals("otherValue6", otherConfig.getProperty("key6", null));

    embeddedApollo.deleteProperty(anotherNamespace, "key6");
    embeddedApollo.deleteProperty(anotherNamespace, "key6");

    assertTrue(changes.tryAcquire(5, TimeUnit.SECONDS));
    assertNull(otherConfig.getProperty("key6", null));
    assertEquals(0, changes.availablePermits());
  }
}
