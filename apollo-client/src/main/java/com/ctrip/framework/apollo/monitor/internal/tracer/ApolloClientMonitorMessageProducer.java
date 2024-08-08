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

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.*;
import static com.ctrip.framework.apollo.monitor.internal.collector.impl.DefaultApolloClientNamespaceApi.DATE_FORMATTER;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.internal.MonitorConstant;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEventPool;
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

  public static final List<String> TAGS = Collections.unmodifiableList(
      Arrays.asList(
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
      )
  );

  @Override
  public void logError(Throwable cause) {
    ApolloConfigMetricsEventPool.getInstance().getEvent(TAG_ERROR)
        .withTag(TAG_ERROR)
        .putAttachment(THROWABLE, cause)
        .push();
  }

  @Override
  public void logError(String message, Throwable cause) {
    ApolloConfigMetricsEventPool.getInstance().getEvent(TAG_ERROR)
        .withTag(TAG_ERROR)
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
        ApolloConfigMetricsEventPool.getInstance().getEvent(METRICS_NAMESPACE_LATEST_UPDATE_TIME)
            .putAttachment(NAMESPACE, namespace)
            .putAttachment(TIMESTAMP, System.currentTimeMillis())
            .withTag(TAG_NAMESPACE)
            .push();
        break;
      }
      case APOLLO_CONFIG_EXCEPTION: {
        logError(new ApolloConfigException(name));
        break;
      }
      case APOLLO_META_SERVICE: {
        ApolloConfigMetricsEventPool.getInstance().getEvent(META_FRESH)
            .withTag(TAG_BOOTSTRAP)
            .putAttachment(META_FRESH,
                DATE_FORMATTER.format(Instant.ofEpochMilli(System.currentTimeMillis())))
            .push();
        break;
      }
      case APOLLO_CONFIG_SERVICES: {
        ApolloConfigMetricsEventPool.getInstance().getEvent(CONFIG_SERVICE_URL)
            .withTag(TAG_BOOTSTRAP)
            .putAttachment(CONFIG_SERVICE_URL, name)
            .push();
        break;
      }
      case APOLLO_CLIENT_VERSION: {
        ApolloConfigMetricsEventPool.getInstance().getEvent(VERSION)
            .withTag(TAG_BOOTSTRAP)
            .putAttachment(VERSION, name)
            .push();
        break;
      }
      case APOLLO_CLIENT_NAMESPACE_TIMEOUT: {
        ApolloConfigMetricsEventPool.getInstance().getEvent(APOLLO_CLIENT_NAMESPACE_TIMEOUT)
            .putAttachment(NAMESPACE, name)
            .withTag(TAG_NAMESPACE).push();
        break;
      }
      case APOLLO_CLIENT_NAMESPACE_NOT_FOUND: {
        ApolloConfigMetricsEventPool.getInstance().getEvent(APOLLO_CLIENT_NAMESPACE_NOT_FOUND)
            .withTag(
                TAG_NAMESPACE).putAttachment(NAMESPACE, name).push();
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
    ApolloConfigMetricsEventPool.getInstance().getEvent(NAMESPACE_RELEASE_KEY)
        .withTag(TAG_NAMESPACE)
        .putAttachment(NAMESPACE_RELEASE_KEY, name)
        .putAttachment(NAMESPACE, namespace)
        .push();
  }

  @Override
  public void logEvent(String type, String name, String status,
      String nameValuePairs) {
    //
  }

  @Override
  public void logMetricsForCount(String name) {
    //按照':'进行分割
    String logMetricsSplit = ":";
    String[] split = name.split(logMetricsSplit);
    int logMetricsSize = 2;
    if (split.length == logMetricsSize) {
      String value = split[1];
      switch (split[0]) {
        case APOLLO_CLIENT_NAMESPACE_USAGE: {
          ApolloConfigMetricsEventPool.getInstance().getEvent(APOLLO_CLIENT_NAMESPACE_USAGE)
              .putAttachment(MonitorConstant.NAMESPACE, value)
              .withTag(TAG_NAMESPACE).push();
          break;
        }
        default:
          break;
      }
    }
  }

  @Override
  public void logMetricsForCount(String name, int count) {

  }

  @Override
  public Transaction newTransaction(String type, String name) {
    return null;
  }

}
