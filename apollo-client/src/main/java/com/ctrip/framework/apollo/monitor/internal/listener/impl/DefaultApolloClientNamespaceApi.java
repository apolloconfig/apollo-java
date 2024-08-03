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
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxNamespaceMBean;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant;
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class DefaultApolloClientNamespaceApi extends
    AbstractApolloClientMetricsEventListener implements
    ApolloClientNamespaceMonitorApi, ApolloClientJmxNamespaceMBean {

  private static final Logger logger = DeferredLoggerFactory.getLogger(
      DefaultApolloClientNamespaceApi.class);
  private final Map<String, Config> m_configs;
  private final Map<String, ConfigFile> m_configFiles;
  private final Map<String, NamespaceMetrics> namespaces = Maps.newConcurrentMap();
  private final List<String> namespace404 = Lists.newCopyOnWriteArrayList();
  private final List<String> namespaceTimeout = Lists.newCopyOnWriteArrayList();

  public DefaultApolloClientNamespaceApi(Map<String, Config> m_configs,
      Map<String, ConfigFile> m_configFiles
  ) {
    super(TAG_NAMESPACE);
    this.m_configs = m_configs;
    this.m_configFiles = m_configFiles;
  }

  @Override
  public void collect0(ApolloConfigMetricsEvent event) {
    String namespace = event.getAttachmentValue(NAMESPACE);
    String eventName = event.getName();

    switch (eventName) {
      case APOLLO_CLIENT_NAMESPACE_NOT_FOUND:
        namespace404.add(namespace);
        break;
      case APOLLO_CLIENT_NAMESPACE_TIMEOUT:
        namespaceTimeout.add(namespace);
        break;
      default:
        NamespaceMetrics namespaceMetrics = namespaces.computeIfAbsent(namespace,
            k -> new NamespaceMetrics());
        handleNamespaceMetricsEvent(event, namespaceMetrics, namespace);
        break;
    }
  }

  private void handleNamespaceMetricsEvent(ApolloConfigMetricsEvent event,
      NamespaceMetrics namespaceMetrics, String namespace) {
    String eventName = event.getName();
    switch (eventName) {
      case APOLLO_CLIENT_NAMESPACE_USAGE:
        namespaceMetrics.incrementUsageCount();
        String mapKey = namespace + ApolloClientMonitorConstant.METRICS_NAMESPACE_USAGE;
        createOrUpdateCounterSample(mapKey, ApolloClientMonitorConstant.METRICS_NAMESPACE_USAGE,
            Collections.singletonMap(NAMESPACE, namespace), 1);
        break;
      case METRICS_NAMESPACE_LATEST_UPDATE_TIME:
        long updateTime = event.getAttachmentValue(ApolloClientMonitorConstant.TIMESTAMP);
        namespaceMetrics.setLatestUpdateTime(updateTime);
        break;
      case APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND:
        long firstLoadSpendTime = event.getAttachmentValue(ApolloClientMonitorConstant.TIMESTAMP);
        namespaceMetrics.setFirstLoadSpend(firstLoadSpendTime);
        break;
      case NAMESPACE_RELEASE_KEY:
        String releaseKey = event.getAttachmentValue(NAMESPACE_RELEASE_KEY);
        namespaceMetrics.setReleaseKey(releaseKey);
        break;
      default:
        logger.warn("Unhandled event name: {}", eventName);
        break;
    }
  }

  @Override
  public void export0() {
    namespaces.forEach((namespace, metrics) -> {
      updateNamespaceGaugeSample(METRICS_NAMESPACE_FIRST_LOAD_SPEND, namespace,
          metrics.getFirstLoadSpend());
      updateNamespaceGaugeSample(METRICS_NAMESPACE_ITEM_NUM, namespace,
          m_configs.get(namespace).getPropertyNames().size());
      updateNamespaceGaugeSample(METRICS_CONFIG_FILE_NUM, namespace, m_configFiles.size());
    });
    createOrUpdateGaugeSample(METRICS_NAMESPACE_NOT_FOUND, METRICS_NAMESPACE_NOT_FOUND,
        Collections.emptyMap(),
        namespace404.size());
    createOrUpdateGaugeSample(METRICS_NAMESPACE_TIMEOUT, METRICS_NAMESPACE_TIMEOUT,
        Collections.emptyMap(), namespaceTimeout.size());
  }

  private void updateNamespaceGaugeSample(String key, String namespace, double value) {
    createOrUpdateGaugeSample(namespace + key, key, Collections.singletonMap(NAMESPACE, namespace),
        value);
  }


  @Override
  public Map<String, NamespaceMetrics> getNamespaceMetrics() {
    return namespaces;
  }


  @Override
  public List<String> getNamespace404() {
    return namespace404;
  }

  @Override
  public List<String> getNamespaceTimeout() {
    return namespaceTimeout;
  }

  @Override
  public List<String> getNamespaceItemName(String namespace) {
    Config config = m_configs.get(namespace);
    return config == null ? Collections.emptyList() : new ArrayList<>(config.getPropertyNames());
  }

  @Override
  public List<String> getAllNamespaceReleaseKey() {
    return namespaces.entrySet().stream()
        .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue().getReleaseKey()))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllNamespaceUsageCount() {
    return namespaces.entrySet().stream()
        .map(entry -> String.format("%s:%d", entry.getKey(), entry.getValue().getUsageCount()))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllNamespacesLatestUpdateTime() {
    return namespaces.entrySet().stream()
        .map(entry -> String.format("%s:%s", entry.getKey(),
            DATE_FORMATTER.format(Instant.ofEpochMilli(entry.getValue().getLatestUpdateTime()))))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllUsedNamespaceName() {
    return new ArrayList<>(namespaces.keySet());
  }

  @Override
  public List<String> getAllNamespaceFirstLoadSpend() {
    return namespaces.entrySet().stream()
        .map(entry -> entry.getKey() + ":" + entry.getValue().getFirstLoadSpend())
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllNamespaceItemName() {
    return m_configs.values().stream()
        .map(config -> config.getPropertyNames().toString())
        .collect(Collectors.toList());
  }

  public static class NamespaceMetrics {

    private int usageCount;
    private long firstLoadSpend;
    private long latestUpdateTime = System.currentTimeMillis();
    private String releaseKey = "default";

    public String getReleaseKey() {
      return releaseKey;
    }

    public void setReleaseKey(String releaseKey) {
      this.releaseKey = releaseKey;
    }

    public int getUsageCount() {
      return usageCount;
    }

    public void incrementUsageCount() {
      this.usageCount++;
    }

    public long getFirstLoadSpend() {
      return firstLoadSpend;
    }

    public void setFirstLoadSpend(long firstLoadSpend) {
      this.firstLoadSpend = firstLoadSpend;
    }

    public long getLatestUpdateTime() {
      return latestUpdateTime;
    }

    public void setLatestUpdateTime(long latestUpdateTime) {
      this.latestUpdateTime = latestUpdateTime;
    }
  }

}
