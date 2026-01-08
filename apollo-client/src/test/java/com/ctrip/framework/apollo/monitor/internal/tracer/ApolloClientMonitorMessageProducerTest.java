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
package com.ctrip.framework.apollo.monitor.internal.tracer;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.tracer.spi.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;


public class ApolloClientMonitorMessageProducerTest {

  private ApolloClientMonitorMessageProducer producer;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    producer = new ApolloClientMonitorMessageProducer();
  }

  @Test
  public void testLogError_Throwable() {
    Throwable cause = new Exception("Test exception");

    producer.logError(cause);
  }

  @Test
  public void testLogError_String_Throwable() {
    String message = "Test error message";
    Throwable cause = new Exception("Test exception");

    producer.logError(message, cause);
  }

  @Test
  public void testLogEvent_TaggedEvent() {
    String type = ApolloClientMonitorMessageProducer.TAGS.get(0); // APOLLO_CLIENT_CONFIGCHANGES
    String name = "Test event";

    producer.logEvent(type, name);
  }

  @Test
  public void testLogEvent_ClientConfigEvent() {
    String type = APOLLO_CLIENT_CONFIGS + "namespace";
    String name = "Test config";

    producer.logEvent(type, name);
  }

  @Test
  public void testLogMetricsForCount() {
    String name = APOLLO_CLIENT_NAMESPACE_USAGE + ":testNamespace";

    producer.logMetricsForCount(name);
  }

  @Test
  public void testNewTransaction() {
    Transaction result = producer.newTransaction("type", "name");

    assertEquals(ApolloClientMessageProducerComposite.NULL_TRANSACTION, result);
  }
}