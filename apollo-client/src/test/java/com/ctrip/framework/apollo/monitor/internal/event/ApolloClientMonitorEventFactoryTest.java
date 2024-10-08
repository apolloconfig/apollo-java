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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

public class ApolloClientMonitorEventFactoryTest {

  private ApolloClientMonitorEventFactory factory;

  @Before
  public void setUp() {
    factory = ApolloClientMonitorEventFactory.getInstance();
  }

  @Test
  public void testGetInstance() {
    ApolloClientMonitorEventFactory instance1 = ApolloClientMonitorEventFactory.getInstance();
    ApolloClientMonitorEventFactory instance2 = ApolloClientMonitorEventFactory.getInstance();

    assertNotNull(instance1);
    assertNotNull(instance2);
    // 验证两个实例是同一个
    assertSame(instance1, instance2);
  }

  @Test
  public void testCreateEvent() {
    String eventName = "TestEvent";
    ApolloClientMonitorEvent event = factory.createEvent(eventName);

    assertNotNull(event);
    assertEquals(eventName, event.getName());
  }
}
