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
package com.ctrip.framework.apollo.monitor.internal.event;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread-safe pool for reusing ApolloConfigMetricsEvent instances. This class improves
 * performance by reducing the number of object creations and garbage collections. It uses a factory
 * to create new instances when needed. The pool size is determined by the configuration value.
 * Usage: ApolloConfigMetricsEventPool eventPool = new ApolloConfigMetricsEventPool(new
 * ApolloConfigMetricsEventFactory()); ApolloConfigMetricsEvent event =
 * eventPool.getEvent("eventName"); // use the event eventPool.returnEvent(event);
 *
 * @author Rawven
 * @date 2024/08/08
 */
public class ApolloConfigMetricsEventPool {

  private static volatile ApolloConfigMetricsEventPool INSTANCE;

  private final int maxPoolSize;
  private final Queue<ApolloConfigMetricsEvent> pool;
  private final ApolloConfigMetricsEventFactory factory;

  /**
   * Constructs a new event pool with the specified factory. The pool size is determined by the
   * configuration.
   */
  private ApolloConfigMetricsEventPool() {
    this.factory = new ApolloConfigMetricsEventFactory();
    this.maxPoolSize = ApolloInjector.getInstance(ConfigUtil.class)
        .getMonitorMetricsEventPoolSize();
    this.pool = new ConcurrentLinkedQueue<>();
    initializePool();
  }

  public static ApolloConfigMetricsEventPool getInstance() {
    if (INSTANCE == null) {
      synchronized (ApolloConfigMetricsEventPool.class) {
        if (INSTANCE == null) {
          INSTANCE = new ApolloConfigMetricsEventPool();
        }
      }
    }
    return INSTANCE;
  }

  /**
   * Initializes the pool with empty events up to one fourth of the maximum pool size.
   */
  private void initializePool() {
    int initialSize = maxPoolSize / 4;
    for (int i = 0; i < initialSize; i++) {
      pool.offer(factory.createEvent(null));
    }
  }

  /**
   * Retrieves an event from the pool. If the pool is empty, a new event is created using the
   * factory.
   *
   * @param name the name for the event
   * @return an ApolloConfigMetricsEvent instance
   */
  public ApolloConfigMetricsEvent getEvent(String name) {
    ApolloConfigMetricsEvent event = pool.poll();
    if (event == null) {
      event = factory.createEvent(name);
    } else {
      event.reset(name);
    }
    return event;
  }

  /**
   * Returns an event back to the pool. If the pool is full, the event is discarded.
   *
   * @param event the event to return
   */
  public void returnEvent(ApolloConfigMetricsEvent event) {
    if (pool.size() < maxPoolSize) {
      pool.offer(event);
    }
  }
}
