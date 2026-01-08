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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ApolloClientMessageProducerCompositeTest {

  private ApolloClientMessageProducerComposite composite;

  @Mock
  private MessageProducer producer1;

  @Mock
  private MessageProducer producer2;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    List<MessageProducer> producers = Arrays.asList(producer1, producer2);
    composite = new ApolloClientMessageProducerComposite(producers);
  }

  @Test
  public void testLogError_Throwable() {
    Throwable cause = new Exception("Test exception");

    composite.logError(cause);

    verify(producer1).logError(cause);
    verify(producer2).logError(cause);
  }

  @Test
  public void testLogError_String_Throwable() {
    String message = "Test error message";
    Throwable cause = new Exception("Test exception");

    composite.logError(message, cause);

    verify(producer1).logError(message, cause);
    verify(producer2).logError(message, cause);
  }

  @Test
  public void testLogEvent_Type_Name() {
    String type = "EVENT_TYPE";
    String name = "EVENT_NAME";

    composite.logEvent(type, name);

    verify(producer1).logEvent(type, name);
    verify(producer2).logEvent(type, name);
  }

  @Test
  public void testLogEvent_Type_Name_Status_NameValuePairs() {
    String type = "EVENT_TYPE";
    String name = "EVENT_NAME";
    String status = "SUCCESS";
    String nameValuePairs = "key=value";

    composite.logEvent(type, name, status, nameValuePairs);

    verify(producer1).logEvent(type, name, status, nameValuePairs);
    verify(producer2).logEvent(type, name, status, nameValuePairs);
  }

  @Test
  public void testLogMetricsForCount() {
    String name = "METRIC_NAME";

    composite.logMetricsForCount(name);

    verify(producer1).logMetricsForCount(name);
    verify(producer2).logMetricsForCount(name);
  }

  @Test
  public void testNewTransaction() {
    String type = "TRANSACTION_TYPE";
    String name = "TRANSACTION_NAME";

    Transaction transaction1 = mock(Transaction.class);
    when(producer1.newTransaction(type, name)).thenReturn(null);
    when(producer2.newTransaction(type, name)).thenReturn(transaction1);

    Transaction result = composite.newTransaction(type, name);

    assertEquals(transaction1, result);
    verify(producer1).newTransaction(type, name);
    verify(producer2).newTransaction(type, name);
  }

  @Test
  public void testNewTransaction_NoValidTransaction() {
    String type = "TRANSACTION_TYPE";
    String name = "TRANSACTION_NAME";

    when(producer1.newTransaction(type, name)).thenReturn(null);
    when(producer2.newTransaction(type, name)).thenReturn(null);

    Transaction result = composite.newTransaction(type, name);

    assertEquals(ApolloClientMessageProducerComposite.NULL_TRANSACTION, result);
    verify(producer1).newTransaction(type, name);
    verify(producer2).newTransaction(type, name);
  }
}
