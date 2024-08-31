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

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.MBEAN_NAME;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.monitor.internal.exporter.AbstractApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.jmx.ApolloClientJmxMBeanRegister;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorContext;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientBootstrapArgsApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientExceptionApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientNamespaceApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientThreadPoolApi;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporterFactory;
import com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMonitorMessageProducer;
import com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMessageProducerComposite;
import com.ctrip.framework.apollo.tracer.internals.NullMessageProducer;
import com.ctrip.framework.apollo.tracer.internals.cat.CatMessageProducer;
import com.ctrip.framework.apollo.tracer.internals.cat.CatNames;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.List;

/**
 * ConfigMonitorInitializer initializes the Apollo Config Monitor.
 */
public class ConfigMonitorInitializer {

  protected static boolean hasInitialized = false;
  private static ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  private static ApolloClientMonitorContext monitorContext = ApolloInjector.getInstance(
      ApolloClientMonitorContext.class);

  public static void initialize() {
    if (m_configUtil.isClientMonitorEnabled() && !hasInitialized) {
      synchronized (ConfigMonitorInitializer.class) {
        if (!hasInitialized) {
          doInit();
          hasInitialized = true;
        }
      }
    }
  }

  private static void doInit() {
    initializeMetricsEventListener();
    initializeMetricsExporter();
    initializeJmxMonitoring();
    hasInitialized = true;
  }


  private static void initializeJmxMonitoring() {
    if (m_configUtil.isClientMonitorJmxEnabled()) {
      monitorContext.getCollectors().forEach(metricsCollector ->
          ApolloClientJmxMBeanRegister.register(
              MBEAN_NAME + metricsCollector.getName(), metricsCollector)
      );
    }
  }

  private static void initializeMetricsEventListener() {
    DefaultConfigManager configManager = (DefaultConfigManager) ApolloInjector.getInstance(
        ConfigManager.class);
    monitorContext.setApolloClientBootstrapArgsMonitorApi(new DefaultApolloClientBootstrapArgsApi(
        m_configUtil));
    monitorContext.setApolloClientExceptionMonitorApi(new DefaultApolloClientExceptionApi());
    monitorContext.setApolloClientNamespaceMonitorApi(new DefaultApolloClientNamespaceApi(
        configManager.m_configs, configManager.m_configFiles));
    monitorContext.setApolloClientThreadPoolMonitorApi(new DefaultApolloClientThreadPoolApi(
        RemoteConfigRepository.m_executorService,
        AbstractConfig.m_executorService, AbstractConfigFile.m_executorService,
        AbstractApolloClientMetricsExporter.m_executorService));
  }

  private static void initializeMetricsExporter(
  ) {
    ApolloClientMetricsExporterFactory exporterFactory = ApolloInjector.getInstance(
        ApolloClientMetricsExporterFactory.class);
    ApolloClientMetricsExporter metricsReporter = exporterFactory.getMetricsReporter(
        monitorContext.getCollectors());
    if(metricsReporter != null) {
      monitorContext.setApolloClientMetricsExporter(metricsReporter);
    }
  }

  public static ApolloClientMessageProducerComposite initializeMessageProducerComposite() {
    List<MessageProducer> producers = ServiceBootstrap.loadAllOrdered(MessageProducer.class);

    if (m_configUtil.isClientMonitorEnabled()) {
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
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  }
}