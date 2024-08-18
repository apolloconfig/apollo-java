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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.DefaultConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListenerManager;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientBootstrapArgsApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientExceptionApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientNamespaceApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientThreadPoolApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientMonitorEventListenerManager;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporterFactory;
import com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMonitorMessageProducer;
import com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMessageProducerComposite;
import com.ctrip.framework.apollo.tracer.internals.NullMessageProducer;
import com.ctrip.framework.apollo.tracer.internals.cat.CatMessageProducer;
import com.ctrip.framework.apollo.tracer.internals.cat.CatNames;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import com.google.common.collect.Lists;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigMonitorInitializer initializes the Apollo Config Monitor.
 */
public class ConfigMonitorInitializer {

  private static final Logger logger = LoggerFactory.getLogger(ConfigMonitorInitializer.class);
  protected static boolean hasInitialized = false;
  private static ConfigUtil CONFIG_UTIL = ApolloInjector.getInstance(ConfigUtil.class);

  public static void initialize() {
    if (CONFIG_UTIL.getClientMonitorEnabled() && !hasInitialized) {
      logger.debug("Initializing ConfigMonitor");
      DefaultApolloClientMonitorEventListenerManager manager = initializeMetricsEventListenerManager();
      List<ApolloClientMonitorEventListener> collectors = initializeCollectors(manager);
      ApolloClientMetricsExporter metricsExporter = initializeMetricsExporter(collectors);
      initializeConfigMonitor(collectors, metricsExporter);
      hasInitialized = true;
      logger.debug("ConfigMonitor initialized successfully.");
    }
  }

  private static DefaultApolloClientMonitorEventListenerManager initializeMetricsEventListenerManager() {
    return (DefaultApolloClientMonitorEventListenerManager) ApolloInjector.getInstance(
        ApolloClientMonitorEventListenerManager.class);
  }

  private static List<ApolloClientMonitorEventListener> initializeCollectors(
      DefaultApolloClientMonitorEventListenerManager manager) {

    DefaultConfigManager configManager = (DefaultConfigManager) ApolloInjector.getInstance(
        ConfigManager.class);

    List<ApolloClientMonitorEventListener> collectors = Lists.newArrayList(
        new DefaultApolloClientExceptionApi(),
        new DefaultApolloClientNamespaceApi(configManager.m_configs, configManager.m_configFiles),
        new DefaultApolloClientThreadPoolApi(RemoteConfigRepository.m_executorService,
            AbstractConfig.m_executorService, AbstractConfigFile.m_executorService),
        new DefaultApolloClientBootstrapArgsApi(CONFIG_UTIL)
    );

    manager.setCollectors(collectors);
    return collectors;
  }

  private static ApolloClientMetricsExporter initializeMetricsExporter(
      List<ApolloClientMonitorEventListener> collectors) {
    ApolloClientMetricsExporterFactory exporterFactory = ApolloInjector.getInstance(
        ApolloClientMetricsExporterFactory.class);
    return exporterFactory.getMetricsReporter(collectors);
  }

  private static void initializeConfigMonitor(List<ApolloClientMonitorEventListener> collectors,
      ApolloClientMetricsExporter metricsExporter) {

    DefaultConfigMonitor configMonitor = (DefaultConfigMonitor) ApolloInjector.getInstance(
        ConfigMonitor.class);
    configMonitor.init(
        (DefaultApolloClientNamespaceApi) collectors.get(1),
        (DefaultApolloClientThreadPoolApi) collectors.get(2),
        (DefaultApolloClientExceptionApi) collectors.get(0),
        (DefaultApolloClientBootstrapArgsApi) collectors.get(3),
        metricsExporter
    );
  }

  public static ApolloClientMessageProducerComposite initializeMessageProducerComposite() {
    List<MessageProducer> producers = ServiceBootstrap.loadAllOrdered(MessageProducer.class);

    if (CONFIG_UTIL.getClientMonitorEnabled()) {
      producers.add(new ApolloClientMonitorMessageProducer());
    }

    if (ClassLoaderUtil.isClassPresent(CatNames.CAT_CLASS)) {
      producers.add(new CatMessageProducer());
    }

    if (producers.isEmpty()) {
      producers.add(new NullMessageProducer());
    }

    return new ApolloClientMessageProducerComposite(producers);
  }
  
  // for test only
  protected static void reset() {
    hasInitialized = false;
    CONFIG_UTIL = ApolloInjector.getInstance(ConfigUtil.class);

  }
}