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

import com.ctrip.framework.apollo.tracer.internals.NullTransaction;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.util.List;

/**
 * message producer composite
 *
 * @author Rawven
 */
public class ApolloClientMessageProducerComposite implements MessageProducer {

  public static final NullTransaction NULL_TRANSACTION = new NullTransaction();
  private final List<MessageProducer> producers;

  public ApolloClientMessageProducerComposite(List<MessageProducer> producers) {
    this.producers = producers;
  }

  @Override
  public void logError(Throwable cause) {
    for (MessageProducer producer : producers) {
      producer.logError(cause);
    }
  }

  @Override
  public void logError(String message, Throwable cause) {
    for (MessageProducer producer : producers) {
      producer.logError(message, cause);
    }
  }

  @Override
  public void logEvent(String type, String name) {
    for (MessageProducer producer : producers) {
      producer.logEvent(type, name);
    }
  }

  @Override
  public void logEvent(String type, String name, String status, String nameValuePairs) {
    for (MessageProducer producer : producers) {
      producer.logEvent(type, name, status, nameValuePairs);
    }
  }

  @Override
  public void logMetricsForCount(String name) {
    for (MessageProducer producer : producers) {
      producer.logMetricsForCount(name);
    }
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    for (MessageProducer producer : producers) {
      Transaction transaction = producer.newTransaction(type, name);
      if (transaction != null) {
        return transaction;
      }
    }
    return NULL_TRANSACTION;
  }
}