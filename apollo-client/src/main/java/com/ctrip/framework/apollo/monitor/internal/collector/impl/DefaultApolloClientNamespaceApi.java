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
package com.ctrip.framework.apollo.monitor.internal.collector.impl;


import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.*;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.jmx.mbean.ApolloClientJmxNamespaceMBean;
import com.ctrip.framework.apollo.monitor.internal.MonitorConstant;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class DefaultApolloClientNamespaceApi extends AbstractMetricsCollector implements
    ApolloClientNamespaceMonitorApi, ApolloClientJmxNamespaceMBean {

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
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
        handleNamespaceMetricsEvent(event, namespaceMetrics);
        break;
    }
  }

  private void handleNamespaceMetricsEvent(ApolloConfigMetricsEvent event,
      NamespaceMetrics namespaceMetrics) {
    String eventName = event.getName();
    switch (eventName) {
      case APOLLO_CLIENT_NAMESPACE_USAGE:
        namespaceMetrics.incrementUsageCount();
        break;
      case METRICS_NAMESPACE_LATEST_UPDATE_TIME:
        long updateTime = event.getAttachmentValue(MonitorConstant.TIMESTAMP);
        namespaceMetrics.setLatestUpdateTime(updateTime);
        break;
      case APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND:
        long firstLoadSpendTime = event.getAttachmentValue(MonitorConstant.TIMESTAMP);
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
      updateCounterSample(METRICS_NAMESPACE_USAGE, namespace, metrics.getUsageCount());
      updateGaugeSample(METRICS_NAMESPACE_FIRST_LOAD_SPEND, namespace,
          metrics.getFirstLoadSpend());
      updateGaugeSample(METRICS_NAMESPACE_LATEST_UPDATE_TIME, namespace,
          metrics.getLatestUpdateTime());
      updateGaugeSample(METRICS_NAMESPACE_ITEM_NUM, namespace,
          m_configs.get(namespace).getPropertyNames().size());
      updateGaugeSample(METRICS_CONFIG_FILE_NUM, namespace, m_configFiles.size());
    });
    updateGaugeSample(METRICS_NAMESPACE_NOT_FOUND, "", namespace404.size());
    updateGaugeSample(METRICS_NAMESPACE_TIMEOUT, "", namespaceTimeout.size());
  }

  private void updateCounterSample(String key, String namespace, double value) {
    String mapKey = namespace + key;
    if (!counterSamples.containsKey(mapKey)) {
      CounterModel builder = CounterModel.create(key, 0);
      if (!namespace.isEmpty()) {
        builder.putTag(NAMESPACE, namespace);
      }
      counterSamples.put(mapKey, builder);
    }
    counterSamples.get(mapKey).updateValue(value);
  }


  private void updateGaugeSample(String key, String namespace, double value) {
    String mapKey = namespace + key;
    if (!gaugeSamples.containsKey(mapKey)) {
      GaugeModel builder = GaugeModel.create(key, 0);
      if (!namespace.isEmpty()) {
        builder.putTag(NAMESPACE, namespace);
      }
      gaugeSamples.put(mapKey, builder);
    }
    gaugeSamples.get(mapKey).updateValue(value);
  }

  @Override
  public Map<String, NamespaceMetrics> getNamespaceMetrics() {
    return namespaces;
  }

  @Override
  public String getNamespaceReleaseKey(String namespace) {
    NamespaceMetrics namespaceMetrics = namespaces.get(namespace);
    return namespaceMetrics == null ? null : namespaceMetrics.getReleaseKey();
  }

  @Override
  public long getNamespaceUsageCount(String namespace) {
    NamespaceMetrics namespaceMetrics = namespaces.get(namespace);
    return namespaceMetrics == null ? 0 : namespaceMetrics.getUsageCount();
  }

  @Override
  public String getNamespaceLatestUpdateTime(String namespace) {
    NamespaceMetrics namespaceMetrics = namespaces.get(namespace);
    return namespaceMetrics == null ? null
        : DATE_FORMATTER.format(Instant.ofEpochMilli(namespaceMetrics.getLatestUpdateTime()));
  }

  @Override
  public long getNamespaceFirstLoadSpend(String namespace) {
    NamespaceMetrics namespaceMetrics = namespaces.get(namespace);
    return namespaceMetrics == null ? 0 : namespaceMetrics.getFirstLoadSpend();
  }

  @Override
  public String getNamespace404() {
    return namespace404.toString();
  }

  @Override
  public String getNamespaceTimeout() {
    return namespaceTimeout.toString();
  }

  @Override
  public List<String> getNamespaceItemName(String namespace) {
    Config config = m_configs.get(namespace);
    return config == null ? Collections.emptyList() : new ArrayList<>(config.getPropertyNames());
  }

  @Override
  public List<String> getAllNamespaceReleaseKey() {
    List<String> releaseKeys = Lists.newArrayList();
    namespaces.forEach((k, v) -> releaseKeys.add(k + ":" + v.getReleaseKey()));
    return releaseKeys;
  }

  @Override
  public List<String> getAllNamespaceUsageCount() {
    List<String> usedTimes = Lists.newArrayList();
    namespaces.forEach((k, v) -> usedTimes.add(k + ":" + v.getUsageCount()));
    return usedTimes;
  }

  @Override
  public List<String> getAllNamespacesLatestUpdateTime() {
    List<String> latestUpdateTimes = Lists.newArrayList();
    namespaces.forEach((k, v) -> latestUpdateTimes.add(
        k + ":" + DATE_FORMATTER.format(Instant.ofEpochMilli(v.getLatestUpdateTime()))));
    return latestUpdateTimes;
  }

  @Override
  public List<String> getAllUsedNamespaceName() {
    ArrayList<String> namespaces = Lists.newArrayList();
    m_configs.forEach((k, v) -> namespaces.add(k));
    return namespaces;
  }

  @Override
  public List<String> getAllNamespaceFirstLoadSpend() {
    List<String> firstLoadSpends = Lists.newArrayList();
    namespaces.forEach((k, v) -> firstLoadSpends.add(
        k + ":" + v.getFirstLoadSpend()));
    return firstLoadSpends;
  }

  @Override
  public List<String> getAllNamespaceItemName() {
    List<String> namespaceItems = Lists.newArrayList();
    m_configs.forEach((k, v) -> namespaceItems.add(v.getPropertyNames().toString()));
    return namespaceItems;
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

    @Override
    public String toString() {
      return "NamespaceMetrics{" +
          "usageCount=" + usageCount +
          ", firstLoadSpend=" + firstLoadSpend +
          ", latestUpdateTime=" + latestUpdateTime +
          ", releaseKey='" + releaseKey + '\'' +
          '}';
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
