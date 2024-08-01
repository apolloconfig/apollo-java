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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.THROWABLE;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DefaultApolloClientExceptionApiTest {

  private DefaultApolloClientExceptionApi exceptionApi;

  @Before
  public void setUp() {
    exceptionApi = new DefaultApolloClientExceptionApi();
  }

  @Test
  public void testCollect0_AddsException() {
    ApolloConfigException exception = new ApolloConfigException("Test Exception");
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getAttachmentValue(THROWABLE)).thenReturn(exception);

    exceptionApi.collect0(event);

    List<Exception> exceptions = exceptionApi.getApolloConfigExceptionList();
    assertEquals(1, exceptions.size());
    assertEquals(exception, exceptions.get(0));
  }

  @Test
  public void testCollect0_IncrementsExceptionCount() {
    ApolloConfigException exception = new ApolloConfigException("Test Exception");
    ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
    when(event.getAttachmentValue(THROWABLE)).thenReturn(exception);

    exceptionApi.collect0(event);
    exceptionApi.collect0(event);

    assertEquals(2, exceptionApi.getApolloConfigExceptionList().size());
  }

  @Test
  public void testGetApolloConfigExceptionDetails() {
    ApolloConfigException exception1 = new ApolloConfigException("First Exception");
    ApolloConfigException exception2 = new ApolloConfigException("Second Exception");

    ApolloClientMonitorEvent event1 = mock(ApolloClientMonitorEvent.class);
    ApolloClientMonitorEvent event2 = mock(ApolloClientMonitorEvent.class);

    when(event1.getAttachmentValue(THROWABLE)).thenReturn(exception1);
    when(event2.getAttachmentValue(THROWABLE)).thenReturn(exception2);

    exceptionApi.collect0(event1);
    exceptionApi.collect0(event2);

    List<String> details = exceptionApi.getApolloConfigExceptionDetails();
    assertEquals(2, details.size());
    assertTrue(details.contains("First Exception"));
    assertTrue(details.contains("Second Exception"));
  }

  @Test
  public void testCollect0_HandlesMaxQueueSize() {
    for (int i = 0; i < 25; i++) {
      ApolloClientMonitorEvent event = mock(ApolloClientMonitorEvent.class);
      when(event.getAttachmentValue(THROWABLE)).thenReturn(
          new ApolloConfigException("Exception " + i));
      exceptionApi.collect0(event);
    }

    assertEquals(25, exceptionApi.getApolloConfigExceptionList().size());

    // Add one more to exceed the size.
    ApolloClientMonitorEvent overflowEvent = mock(ApolloClientMonitorEvent.class);
    when(overflowEvent.getAttachmentValue(THROWABLE)).thenReturn(
        new ApolloConfigException("Overflow Exception"));
    exceptionApi.collect0(overflowEvent);

    assertEquals(25, exceptionApi.getApolloConfigExceptionList().size());
  }
}