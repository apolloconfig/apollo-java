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
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  private final Map<String, Config> m_configs;
  private final Map<String, ConfigFile> m_configFiles;
  private final Map<String, NamespaceMetrics> namespaces = Maps.newConcurrentMap();
  private final Set<String> namespace404 = Sets.newCopyOnWriteArraySet();
  private final Set<String> namespaceTimeout = Sets.newCopyOnWriteArraySet();

  public DefaultApolloClientNamespaceApi(Map<String, Config> m_configs,
      Map<String, ConfigFile> m_configFiles
  ) {
    super(TAG_NAMESPACE);
    this.m_configs = m_configs;
    this.m_configFiles = m_configFiles;
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

  private void handleNamespaceNotFound(String namespace) {
    namespace404.add(namespace);
  }

  private void handleNamespaceTimeout(String namespace) {
    namespaceTimeout.add(namespace);
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

  private void handleUsageEvent(NamespaceMetrics namespaceMetrics, String namespace) {
    namespaceMetrics.incrementUsageCount();
    String mapKey = namespace + ApolloClientMonitorConstant.METRICS_NAMESPACE_USAGE;
    createOrUpdateCounterSample(mapKey, ApolloClientMonitorConstant.METRICS_NAMESPACE_USAGE,
        Collections.singletonMap(NAMESPACE, namespace), 1);
  }

  private void handleUpdateTimeEvent(ApolloClientMonitorEvent event,
      NamespaceMetrics namespaceMetrics) {
    long updateTime = event.getAttachmentValue(ApolloClientMonitorConstant.TIMESTAMP);
    namespaceMetrics.setLatestUpdateTime(updateTime);
  }

  private void handleFirstLoadSpendEvent(ApolloClientMonitorEvent event,
      NamespaceMetrics namespaceMetrics) {
    long firstLoadSpendTime = event.getAttachmentValue(ApolloClientMonitorConstant.TIMESTAMP);
    namespaceMetrics.setFirstLoadSpend(firstLoadSpendTime);
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
      createOrUpdateGaugeSample(namespace + METRICS_NAMESPACE_FIRST_LOAD_SPEND,
          METRICS_NAMESPACE_FIRST_LOAD_SPEND,
          Collections.singletonMap(NAMESPACE, namespace),
          metrics.getFirstLoadSpend());

      createOrUpdateGaugeSample(namespace + METRICS_NAMESPACE_ITEM_NUM,
          METRICS_NAMESPACE_ITEM_NUM,
          Collections.singletonMap(NAMESPACE, namespace),
          m_configs.get(namespace).getPropertyNames().size());
    });

    //  update ConfigFile num
    createOrUpdateGaugeSample(METRICS_CONFIG_FILE_NUM,
        METRICS_CONFIG_FILE_NUM,
        Collections.emptyMap(),
        m_configFiles.size());

    //  update NamespaceStatus metrics
    createOrUpdateGaugeSample(METRICS_NAMESPACE_NOT_FOUND,
        METRICS_NAMESPACE_NOT_FOUND,
        Collections.emptyMap(),
        namespace404.size());

    createOrUpdateGaugeSample(METRICS_NAMESPACE_TIMEOUT,
        METRICS_NAMESPACE_TIMEOUT,
        Collections.emptyMap(),
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
  public Integer getNamespaceItemsNum(String namespace) {
    Config config = m_configs.get(namespace);
    return (config != null) ? config.getPropertyNames().size() : 0;
  }

  @Override
  public Integer getConfigFileNum() {
    return m_configFiles.size();
  }

  public static class NamespaceMetrics {

    private int usageCount;
    private long firstLoadSpend;
    private long latestUpdateTime = System.currentTimeMillis();
    private String releaseKey = "";

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
      usageCount++;
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
