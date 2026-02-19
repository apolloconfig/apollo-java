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
package com.ctrip.framework.apollo.internals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.config.CachedCompositePropertySource;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * @author wxq
 */
public class AbstractConfigTest {

  private static String someAppId = "someAppId";

  /**
   * @see AbstractConfig#fireConfigChange(ConfigChangeEvent)
   */
  @Test
  public void testFireConfigChange_cannot_notify() throws InterruptedException {
    AbstractConfig abstractConfig = spy(new ErrorConfig());
    final String namespace = "app-namespace-0";

    ConfigChangeListener configChangeListener = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {

      }
    });
    abstractConfig
        .addChangeListener(configChangeListener, Collections.singleton("cannot-be-match-key"));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put("key1",
        new ConfigChange(someAppId, namespace, "key1", null, "new-value", PropertyChangeType.ADDED));
    ConfigChangeEvent configChangeEvent = new ConfigChangeEvent(someAppId, namespace, changes);

    abstractConfig.fireConfigChange(configChangeEvent);
    abstractConfig.fireConfigChange(someAppId, namespace, changes);

    // wait a minute for invoking
    Thread.sleep(100);

    verify(configChangeListener, times(0)).onChange(any());
  }

  @Test
  public void testFireConfigChange_event_notify_once()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-1";
    final String key = "great-key";

    final SettableFuture<ConfigChangeEvent> future1 = SettableFuture.create();
    final SettableFuture<ConfigChangeEvent> future2 = SettableFuture.create();

    final AtomicInteger invokeCount = new AtomicInteger();

    final ConfigChangeListener configChangeListener1 = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        invokeCount.incrementAndGet();
        future1.set(changeEvent);
      }
    });
    final ConfigChangeListener configChangeListener2 = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        invokeCount.incrementAndGet();
        future2.set(changeEvent);
      }
    });
    abstractConfig.addChangeListener(configChangeListener1, Collections.singleton(key));
    abstractConfig.addChangeListener(configChangeListener2, Collections.singleton(key));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(key,
        new ConfigChange(someAppId, namespace, key, "old-value", "new-value", PropertyChangeType.MODIFIED));
    ConfigChangeEvent configChangeEvent = new ConfigChangeEvent(someAppId, namespace, changes);

    abstractConfig.fireConfigChange(configChangeEvent);

    assertEquals(configChangeEvent, future1.get(500, TimeUnit.MILLISECONDS));
    assertEquals(configChangeEvent, future2.get(500, TimeUnit.MILLISECONDS));

    assertEquals(2, invokeCount.get());

    verify(configChangeListener1, times(1)).onChange(eq(configChangeEvent));
    verify(configChangeListener2, times(1)).onChange(eq(configChangeEvent));
  }

  @Test
  public void testFireConfigChange_twoCachedCompositePropertySourcesWithSameName_shouldBothBeNotified()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-listener-equals";
    final String key = "great-key";

    ListenerPair listenerPair = createSameNameListeners();
    final CountingCachedCompositePropertySource listener1 = listenerPair.listener1;
    final CountingCachedCompositePropertySource listener2 = listenerPair.listener2;

    abstractConfig.addChangeListener(listener1, Collections.singleton(key));
    abstractConfig.addChangeListener(listener2, Collections.singleton(key));

    Map<String, ConfigChange> changes = createSingleKeyChanges(namespace, key);

    abstractConfig.fireConfigChange(someAppId, namespace, changes);

    assertEquals(Collections.singleton(key), listener1.awaitChange(500, TimeUnit.MILLISECONDS).changedKeys());
    assertEquals(Collections.singleton(key), listener2.awaitChange(500, TimeUnit.MILLISECONDS).changedKeys());

    assertEquals(1, listener1.changeCount.get());
    assertEquals(1, listener2.changeCount.get());
  }

  @Test
  public void testFireConfigChange_twoCachedCompositePropertySourcesWithSameNameAndDifferentInterestedKeys_shouldNotConflict()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-listener-interested-keys";
    final String key1 = "great-key-1";
    final String key2 = "great-key-2";

    ListenerPair listenerPair = createSameNameListeners();
    final CountingCachedCompositePropertySource listener1 = listenerPair.listener1;
    final CountingCachedCompositePropertySource listener2 = listenerPair.listener2;

    abstractConfig.addChangeListener(listener1, Collections.singleton(key1));
    abstractConfig.addChangeListener(listener2, Collections.singleton(key2));

    abstractConfig.fireConfigChange(someAppId, namespace, createSingleKeyChanges(namespace, key1));

    assertEquals(Collections.singleton(key1), listener1.awaitChange(500, TimeUnit.MILLISECONDS).changedKeys());
    assertThrows(TimeoutException.class, () -> listener2.awaitChange(200, TimeUnit.MILLISECONDS));

    listener1.resetChangeFuture();
    listener2.resetChangeFuture();

    abstractConfig.fireConfigChange(someAppId, namespace, createSingleKeyChanges(namespace, key2));

    assertThrows(TimeoutException.class, () -> listener1.awaitChange(200, TimeUnit.MILLISECONDS));
    assertEquals(Collections.singleton(key2), listener2.awaitChange(500, TimeUnit.MILLISECONDS).changedKeys());

    assertEquals(1, listener1.changeCount.get());
    assertEquals(1, listener2.changeCount.get());
  }

  @Test
  public void testRemoveChangeListener_twoCachedCompositePropertySourcesWithSameName_shouldRemoveSpecifiedInstance()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-listener-remove";
    final String key = "great-key";

    ListenerPair listenerPair = createSameNameListeners();
    final CountingCachedCompositePropertySource listener1 = listenerPair.listener1;
    final CountingCachedCompositePropertySource listener2 = listenerPair.listener2;

    abstractConfig.addChangeListener(listener1, Collections.singleton(key));
    abstractConfig.addChangeListener(listener2, Collections.singleton(key));

    assertTrue(abstractConfig.removeChangeListener(listener2));

    abstractConfig.fireConfigChange(someAppId, namespace, createSingleKeyChanges(namespace, key));

    assertEquals(Collections.singleton(key), listener1.awaitChange(500, TimeUnit.MILLISECONDS).changedKeys());
    assertThrows(TimeoutException.class, () -> listener2.awaitChange(200, TimeUnit.MILLISECONDS));

    assertEquals(1, listener1.changeCount.get());
    assertEquals(0, listener2.changeCount.get());
  }

  @Test
  public void testFireConfigChange_changes_notify_once()
      throws ExecutionException, InterruptedException, TimeoutException {
    AbstractConfig abstractConfig = new ErrorConfig();
    final String namespace = "app-namespace-1";
    final String key = "great-key";

    final SettableFuture<ConfigChangeEvent> future1 = SettableFuture.create();
    final SettableFuture<ConfigChangeEvent> future2 = SettableFuture.create();

    final AtomicInteger invokeCount = new AtomicInteger();

    final ConfigChangeListener configChangeListener1 = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        invokeCount.incrementAndGet();
        future1.set(changeEvent);
      }
    });
    final ConfigChangeListener configChangeListener2 = spy(new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        invokeCount.incrementAndGet();
        future2.set(changeEvent);
      }
    });
    abstractConfig.addChangeListener(configChangeListener1, Collections.singleton(key));
    abstractConfig.addChangeListener(configChangeListener2, Collections.singleton(key));

    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(key,
        new ConfigChange(someAppId, namespace, key, "old-value", "new-value", PropertyChangeType.MODIFIED));

    abstractConfig.fireConfigChange(someAppId, namespace, changes);

    assertEquals(Collections.singleton(key), future1.get(500, TimeUnit.MILLISECONDS).changedKeys());
    assertEquals(Collections.singleton(key), future2.get(500, TimeUnit.MILLISECONDS).changedKeys());

    assertEquals(2, invokeCount.get());

    verify(configChangeListener1, times(1)).onChange(any());
    verify(configChangeListener2, times(1)).onChange(any());
  }

  /**
   * Only for current test usage.
   *
   * @author wxq
   */
  private static class ErrorConfig extends AbstractConfig {

    @Override
    public String getProperty(String key, String defaultValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPropertyNames() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ConfigSourceType getSourceType() {
      throw new UnsupportedOperationException();
    }
  }

  private static class CountingCachedCompositePropertySource extends CachedCompositePropertySource {
    private final AtomicInteger changeCount = new AtomicInteger();
    private volatile SettableFuture<ConfigChangeEvent> changeFuture = SettableFuture.create();

    private CountingCachedCompositePropertySource(String name) {
      super(name);
    }

    @Override
    public void onChange(ConfigChangeEvent changeEvent) {
      changeCount.incrementAndGet();
      changeFuture.set(changeEvent);
      super.onChange(changeEvent);
    }

    private void resetChangeFuture() {
      changeFuture = SettableFuture.create();
    }

    private ConfigChangeEvent awaitChange(long timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
      return changeFuture.get(timeout, unit);
    }
  }

  private static ListenerPair createSameNameListeners() {
    CountingCachedCompositePropertySource listener1 =
        new CountingCachedCompositePropertySource("ApolloBootstrapPropertySources");
    CountingCachedCompositePropertySource listener2 =
        new CountingCachedCompositePropertySource("ApolloBootstrapPropertySources");
    assertNotSame(listener1, listener2);
    assertEquals(listener1, listener2);
    return new ListenerPair(listener1, listener2);
  }

  private static class ListenerPair {
    private final CountingCachedCompositePropertySource listener1;
    private final CountingCachedCompositePropertySource listener2;

    private ListenerPair(CountingCachedCompositePropertySource listener1,
        CountingCachedCompositePropertySource listener2) {
      this.listener1 = listener1;
      this.listener2 = listener2;
    }
  }

  private static Map<String, ConfigChange> createSingleKeyChanges(String namespace, String key) {
    Map<String, ConfigChange> changes = new HashMap<>();
    changes.put(key,
        new ConfigChange(someAppId, namespace, key, "old-value", "new-value", PropertyChangeType.MODIFIED));
    return changes;
  }
}
