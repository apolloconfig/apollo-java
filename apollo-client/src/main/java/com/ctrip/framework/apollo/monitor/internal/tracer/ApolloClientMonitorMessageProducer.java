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
import static com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMessageProducerComposite.NULL_TRANSACTION;


import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEventFactory;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Rawven
 */
public class ApolloClientMonitorMessageProducer implements MessageProducer {

  public static final List<String> TAGS = Collections.unmodifiableList(Arrays.asList(
      APOLLO_CLIENT_CONFIGCHANGES,
      APOLLO_CONFIG_EXCEPTION,
      APOLLO_META_SERVICE,
      APOLLO_CONFIG_SERVICES,
      APOLLO_CLIENT_VERSION,
      APOLLO_CONFIGSERVICE,
      APOLLO_CLIENT_CONFIGMETA,
      APOLLO_CLIENT_NAMESPACE_TIMEOUT,
      APOLLO_CLIENT_NAMESPACE_USAGE,
      APOLLO_CLIENT_NAMESPACE_NOT_FOUND
  ));

  @Override
  public void logError(Throwable cause) {
    publishErrorEvent(TAG_ERROR, cause);
  }

  @Override
  public void logError(String message, Throwable cause) {
    publishErrorEvent(TAG_ERROR, cause);
  }

  @Override
  public void logEvent(String type, String name) {
    if (TAGS.contains(type)) {
      handleTaggedEvent(type, name);
    } else if (type.startsWith(APOLLO_CLIENT_CONFIGS)) {
      handleClientConfigEvent(type, name);
    } else if (type.startsWith(APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND)) {
      handleFirstLoadTimeEvent(type, name);
    }
  }

  private void publishErrorEvent(String tag, Throwable cause) {
    ApolloClientMonitorEventFactory.getInstance().createEvent(tag)
        .withTag(tag)
        .putAttachment(THROWABLE, cause)
        .publish();
  }

  private void handleTaggedEvent(String type, String name) {
    switch (type) {
      case APOLLO_CONFIGSERVICE:
        name = name.substring(HELP_STR.length());
        // fall through
      case APOLLO_CLIENT_CONFIGCHANGES:
        publishConfigChangeEvent(name);
        break;
      case APOLLO_CONFIG_EXCEPTION:
        logError(new ApolloConfigException(name));
        break;
      case APOLLO_META_SERVICE:
        publishMetaServiceEvent();
        break;
      case APOLLO_CONFIG_SERVICES:
        publishConfigServiceEvent(name);
        break;
      case APOLLO_CLIENT_VERSION:
        publishClientVersionEvent(name);
        break;
      case APOLLO_CLIENT_NAMESPACE_TIMEOUT:
        publishNamespaceTimeoutEvent(name);
        break;
      case APOLLO_CLIENT_NAMESPACE_NOT_FOUND:
        publishNamespaceNotFoundEvent(name);
        break;
      case APOLLO_CLIENT_CONFIGMETA:
        // 不需要收集
        break;
      default:
        break;
    }
  }

  private void publishConfigChangeEvent(String name) {
    ApolloClientMonitorEventFactory.getInstance()
        .createEvent(METRICS_NAMESPACE_LATEST_UPDATE_TIME)
        .putAttachment(NAMESPACE, name)
        .putAttachment(TIMESTAMP, System.currentTimeMillis())
        .withTag(TAG_NAMESPACE)
        .publish();
  }

  private void publishMetaServiceEvent() {
    ApolloClientMonitorEventFactory.getInstance().createEvent(META_FRESH)
        .withTag(TAG_BOOTSTRAP)
        .putAttachment(META_FRESH, DATE_FORMATTER.format(Instant.now()))
        .publish();
  }

  private void publishConfigServiceEvent(String name) {
    ApolloClientMonitorEventFactory.getInstance().createEvent(CONFIG_SERVICE_URL)
        .withTag(TAG_BOOTSTRAP)
        .putAttachment(CONFIG_SERVICE_URL, name)
        .publish();
  }

  private void publishClientVersionEvent(String name) {
    ApolloClientMonitorEventFactory.getInstance().createEvent(VERSION)
        .withTag(TAG_BOOTSTRAP)
        .putAttachment(VERSION, name)
        .publish();
  }

  private void publishNamespaceTimeoutEvent(String name) {
    ApolloClientMonitorEventFactory.getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_TIMEOUT)
        .putAttachment(NAMESPACE, name)
        .withTag(TAG_NAMESPACE)
        .publish();
  }

  private void publishNamespaceNotFoundEvent(String name) {
    ApolloClientMonitorEventFactory.getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_NOT_FOUND)
        .withTag(TAG_NAMESPACE)
        .putAttachment(NAMESPACE, name)
        .publish();
  }

  private void handleClientConfigEvent(String type, String name) {
    String namespace = type.substring(APOLLO_CLIENT_CONFIGS.length());
    ApolloClientMonitorEventFactory.getInstance().createEvent(NAMESPACE_RELEASE_KEY)
        .withTag(TAG_NAMESPACE)
        .putAttachment(NAMESPACE_RELEASE_KEY, name)
        .putAttachment(NAMESPACE, namespace)
        .publish();
  }

  private void handleFirstLoadTimeEvent(String type, String name) {
    String namespace = type.substring(APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND.length());
    long firstLoadTime = Long.parseLong(name);
    ApolloClientMonitorEventFactory.getInstance()
        .createEvent(APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND)
        .putAttachment(NAMESPACE, namespace)
        .putAttachment(TIMESTAMP, firstLoadTime)
        .withTag(TAG_NAMESPACE)
        .publish();
  }

  @Override
  public void logEvent(String type, String name, String status, String nameValuePairs) {
    // ignore
  }

  @Override
  public void logMetricsForCount(String name) {
    String[] split = name.split(":");
    if (split.length == 2 && APOLLO_CLIENT_NAMESPACE_USAGE.equals(split[0])) {
      ApolloClientMonitorEventFactory.getInstance().createEvent(APOLLO_CLIENT_NAMESPACE_USAGE)
          .putAttachment(ApolloClientMonitorConstant.NAMESPACE, split[1])
          .withTag(TAG_NAMESPACE)
          .publish();
    }
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    return NULL_TRANSACTION;
  }
}