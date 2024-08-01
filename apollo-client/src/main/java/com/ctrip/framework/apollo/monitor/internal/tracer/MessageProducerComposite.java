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
import java.util.Objects;

/**
 * message producer composite
 *
 * @author Rawven
 */
public class MessageProducerComposite implements MessageProducer {

  public static final String ERROR_METRICS = "errorMetrics";
  public static final String THROWABLE = ERROR_METRICS + ".throwable";
  public static final String APOLLO_CLIENT_CONFIGCHANGES = "Apollo.Client.ConfigChanges";
  public static final String APOLLO_CONFIG_EXCEPTION = "ApolloConfigException";
  public static final String APOLLO_META_SERVICE = "Apollo.MetaService";
  public static final String APOLLO_CONFIG_SERVICES = "Apollo.Config.Services";
  public static final String APOLLO_CLIENT_VERSION = "Apollo.Client.Version";
  public static final String APOLLO_CONFIGSERVICE = "Apollo.ConfigService";
  public static final String APOLLO_CLIENT_CONFIGS = "Apollo.Client.Configs.";
  public static final String APOLLO_CLIENT_CONFIGMETA = "Apollo.Client.ConfigMeta";
  public static final String HELP_STR = "periodicRefresh: ";
  private static final NullTransaction NULL_TRANSACTION = new NullTransaction();
  private List<MessageProducer> producers;

  public MessageProducerComposite(List<MessageProducer> producers) {
    this.producers = producers;
  }


  @Override
  public void logError(Throwable cause) {
    producers.forEach(producer -> producer.logError(cause));
  }

  @Override
  public void logError(String message, Throwable cause) {
    producers.forEach(producer -> producer.logError(message, cause));
  }

  @Override
  public void logEvent(String type, String name) {
    producers.forEach(producer -> producer.logEvent(type, name));
  }

  @Override
  public void logEvent(String type, String name, String status,
      String nameValuePairs) {
    producers.forEach(producer -> producer.logEvent(type, name, status, nameValuePairs));
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    return producers.stream()
        .map(producer -> producer.newTransaction(type, name))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(NULL_TRANSACTION);
  }

  public List<MessageProducer> getProducers() {
    return producers;
  }
}
