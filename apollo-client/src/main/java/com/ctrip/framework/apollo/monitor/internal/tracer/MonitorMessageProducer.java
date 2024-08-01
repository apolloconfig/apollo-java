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

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.NAMESPACE;
import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.TIMESTAMP;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloNamespaceCollector.DATE_FORMATTER;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloNamespaceCollector.NAMESPACE_LATEST_UPDATE_TIME;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloNamespaceCollector.NAMESPACE_MONITOR;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloNamespaceCollector.NAMESPACE_RELEASE_KEY;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloRunningParamsCollector.CONFIG_SERVICE_URL;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloRunningParamsCollector.META_FRESH;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloRunningParamsCollector.RUNNING_PARAMS;
import static com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultApolloRunningParamsCollector.VERSION;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CLIENT_CONFIGCHANGES;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CLIENT_CONFIGMETA;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CLIENT_CONFIGS;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CLIENT_VERSION;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CONFIGSERVICE;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CONFIG_EXCEPTION;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_CONFIG_SERVICES;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.APOLLO_META_SERVICE;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.ERROR_METRICS;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.HELP_STR;
import static com.ctrip.framework.apollo.monitor.internal.tracer.MessageProducerComposite.THROWABLE;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * metrics message producer
 *
 * @author Rawven
 */
public class MonitorMessageProducer implements MessageProducer {

  public static final List<String> TAGS = Collections.unmodifiableList(
      Arrays.asList(
          APOLLO_CLIENT_CONFIGCHANGES,
          APOLLO_CONFIG_EXCEPTION,
          APOLLO_META_SERVICE,
          APOLLO_CONFIG_SERVICES,
          APOLLO_CLIENT_VERSION,
          APOLLO_CONFIGSERVICE,
          APOLLO_CLIENT_CONFIGMETA
      )
  );

  @Override
  public void logError(Throwable cause) {
    MetricsEvent.builder().withName(ERROR_METRICS)
        .withTag(ERROR_METRICS)
        .putAttachment(THROWABLE, cause)
        .push();
  }

  @Override
  public void logError(String message, Throwable cause) {
    MetricsEvent.builder().withName(ERROR_METRICS)
        .withTag(ERROR_METRICS)
        .putAttachment(THROWABLE, cause).push();
  }

  @Override
  public void logEvent(String type, String name) {
    if (TAGS.contains(type)) {
      handleTaggedEvent(type, name);
    } else if (type.startsWith(APOLLO_CLIENT_CONFIGS)) {
      handleClientConfigEvent(type, name);
    }
  }

  private void handleTaggedEvent(String type, String name) {
    String namespace;
    switch (type) {
      case APOLLO_CONFIGSERVICE: {
        name = name.substring(HELP_STR.length());
      }
      case APOLLO_CLIENT_CONFIGCHANGES: {
        namespace = name;
        MetricsEvent.builder()
            .withName(NAMESPACE_LATEST_UPDATE_TIME)
            .putAttachment(NAMESPACE, namespace)
            .putAttachment(TIMESTAMP, System.currentTimeMillis())
            .withTag(NAMESPACE_MONITOR)
            .push();
        break;
      }
      case APOLLO_CONFIG_EXCEPTION: {
        logError(new ApolloConfigException(name));
        break;
      }
      case APOLLO_META_SERVICE: {
        MetricsEvent.builder()
            .withName(META_FRESH)
            .withTag(RUNNING_PARAMS)
            .putAttachment(META_FRESH,
                DATE_FORMATTER.format(Instant.ofEpochMilli(System.currentTimeMillis())))
            .push();
        break;
      }
      case APOLLO_CONFIG_SERVICES: {
        MetricsEvent.builder()
            .withName(CONFIG_SERVICE_URL)
            .withTag(RUNNING_PARAMS)
            .putAttachment(CONFIG_SERVICE_URL, name)
            .push();
        break;
      }
      case APOLLO_CLIENT_VERSION: {
        MetricsEvent.builder()
            .withName(VERSION)
            .withTag(RUNNING_PARAMS)
            .putAttachment(VERSION, name)
            .push();
        break;
      }
      case APOLLO_CLIENT_CONFIGMETA:
        // 不需要收集
        break;
      default:
        break;
    }
  }

  private void handleClientConfigEvent(String type, String name) {
    int len = APOLLO_CLIENT_CONFIGS.length();
    String namespace = type.substring(len);
    String releaseKey = name;
    MetricsEvent.builder()
        .withName(NAMESPACE_RELEASE_KEY)
        .withTag(NAMESPACE_MONITOR)
        .putAttachment(NAMESPACE_RELEASE_KEY, releaseKey)
        .putAttachment(NAMESPACE, namespace)
        .push();
  }

  @Override
  public void logEvent(String type, String name, String status,
      String nameValuePairs) {
    //
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    return null;
  }

}
