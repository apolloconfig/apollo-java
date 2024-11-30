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


import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxNamespaceMBean;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant;
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.util.date.DateUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class DefaultApolloClientNamespaceApi extends
    AbstractApolloClientMonitorEventListener implements
    ApolloClientNamespaceMonitorApi, ApolloClientJmxNamespaceMBean {

  private static final Logger logger = DeferredLoggerFactory.getLogger(
      DefaultApolloClientNamespaceApi.class);
  private final ConfigManager configManager;
  private final Map<String, NamespaceMetrics> namespaces = Maps.newConcurrentMap();
  private final Set<String> namespace404 = Sets.newCopyOnWriteArraySet();
  private final Set<String> namespaceTimeout = Sets.newCopyOnWriteArraySet();

  public DefaultApolloClientNamespaceApi(ConfigManager configManager
  ) {
    super(TAG_NAMESPACE);
    this.configManager = configManager;
  }

  @Override
  public void collect0(ApolloClientMonitorEvent event) {
    String namespace = event.getAttachmentValue(NAMESPACE);
    String eventName = event.getName();

    switch (eventName) {
      case APOLLO_CLIENT_NAMESPACE_NOT_FOUND:
        handleNamespaceNotFound(namespace);
        break;
      case APOLLO_CLIENT_NAMESPACE_TIMEOUT:
        handleNamespaceTimeout(namespace);
        break;
      default:
        handleNormalNamespace(namespace, event);
        break;
    }
  }

  private void handleNormalNamespace(String namespace, ApolloClientMonitorEvent event) {
    namespace404.remove(namespace);
    namespaceTimeout.remove(namespace);
    NamespaceMetrics namespaceMetrics = namespaces.computeIfAbsent(namespace,
        k -> new NamespaceMetrics());
    collectMetrics(event, namespaceMetrics, namespace);
  }

  private void collectMetrics(ApolloClientMonitorEvent event, NamespaceMetrics namespaceMetrics,
      String namespace) {
    String eventName = event.getName();
    switch (eventName) {
      case APOLLO_CLIENT_NAMESPACE_USAGE:
        handleUsageEvent(namespaceMetrics, namespace);
        break;
      case METRICS_NAMESPACE_LATEST_UPDATE_TIME:
        handleUpdateTimeEvent(event, namespaceMetrics);
        break;
      case APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND:
        handleFirstLoadSpendEvent(event, namespaceMetrics);
        break;
      case NAMESPACE_RELEASE_KEY:
        handleReleaseKeyEvent(event, namespaceMetrics);
        break;
      default:
        logger.warn("Unhandled event name: {}", eventName);
        break;
    }
  }

  private void handleNamespaceNotFound(String namespace) {
    namespace404.add(namespace);
  }

  private void handleNamespaceTimeout(String namespace) {
    namespaceTimeout.add(namespace);
  }


  private void handleUsageEvent(NamespaceMetrics namespaceMetrics, String namespace) {
    namespaceMetrics.incrementUsageCount();
    createOrUpdateCounterSample(ApolloClientMonitorConstant.METRICS_NAMESPACE_USAGE,
        new String[]{NAMESPACE}, new String[]{namespace}, 1);
  }

  private void handleUpdateTimeEvent(ApolloClientMonitorEvent event,
      NamespaceMetrics namespaceMetrics) {
    namespaceMetrics.setLatestUpdateTime(LocalDateTime.now());
  }

  private void handleFirstLoadSpendEvent(ApolloClientMonitorEvent event,
      NamespaceMetrics namespaceMetrics) {
    long firstLoadSpendTime = event.getAttachmentValue(ApolloClientMonitorConstant.TIMESTAMP);
    namespaceMetrics.setFirstLoadTimeSpendInMs(firstLoadSpendTime);
  }

  private void handleReleaseKeyEvent(ApolloClientMonitorEvent event,
      NamespaceMetrics namespaceMetrics) {
    String releaseKey = event.getAttachmentValue(NAMESPACE_RELEASE_KEY);
    namespaceMetrics.setReleaseKey(releaseKey);
  }

  @Override
  public void export0() {
    namespaces.forEach((namespace, metrics) -> {
      // update NamespaceMetrics
      createOrUpdateGaugeSample(
          METRICS_NAMESPACE_FIRST_LOAD_SPEND,
          new String[]{NAMESPACE}, new String[]{namespace},
          metrics.getFirstLoadTimeSpendInMs());

      createOrUpdateGaugeSample(
          METRICS_NAMESPACE_ITEM_NUM,
          new String[]{NAMESPACE}, new String[]{namespace},
          configManager.getConfig(namespace).getPropertyNames().size());
    });

    //  update NamespaceStatus metrics
    createOrUpdateGaugeSample(METRICS_NAMESPACE_NOT_FOUND,
        namespace404.size());

    createOrUpdateGaugeSample(METRICS_NAMESPACE_TIMEOUT,
        namespaceTimeout.size());
  }

  @Override
  public Map<String, NamespaceMetrics> getNamespaceMetrics() {
    return Collections.unmodifiableMap(namespaces);
  }

  @Override
  public List<String> getNotFoundNamespaces() {
    return new ArrayList<>(namespace404);
  }

  @Override
  public List<String> getTimeoutNamespaces() {
    return new ArrayList<>(namespaceTimeout);
  }

  @Override
  public Map<String, NamespaceMetricsString> getNamespaceMetricsString() {
    Map<String, NamespaceMetricsString> namespaceMetricsStringMap = Maps.newHashMap();
    namespaces.forEach((namespace, metrics) -> {
      NamespaceMetricsString namespaceMetricsString = new NamespaceMetricsString();
      namespaceMetricsString.setFirstLoadTimeSpendInMs(metrics.getFirstLoadTimeSpendInMs());
      DateUtil.formatLocalDateTime(metrics.getLatestUpdateTime())
              .ifPresent(namespaceMetricsString::setLatestUpdateTime);
      namespaceMetricsString.setUsageCount(metrics.getUsageCount());
      namespaceMetricsString.setReleaseKey(metrics.getReleaseKey());
      namespaceMetricsStringMap.put(namespace, namespaceMetricsString);
    });
    return namespaceMetricsStringMap;
  }

  @Override
  public Integer getNamespacePropertySize(String namespace) {
    Config config = configManager.getConfig(namespace);
    return (config != null) ? config.getPropertyNames().size() : 0;
  }

}
