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
import com.ctrip.framework.apollo.core.utils.StringUtils;
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
import com.google.common.collect.Lists;
import java.util.List;

/**
 * ConfigMonitorInitializer initializes the Apollo Config Monitor.
 */
public class ConfigMonitorInitializer {

  private static final ApolloClientMonitorContext MONITOR_CONTEXT = ApolloInjector.getInstance(
      ApolloClientMonitorContext.class);
  protected static volatile boolean hasInitialized = false;
  private static ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

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
  }


  private static void initializeJmxMonitoring() {
    if (m_configUtil.isClientMonitorJmxEnabled()) {
      MONITOR_CONTEXT.getApolloClientMonitorEventListeners().forEach(metricsListener ->
          ApolloClientJmxMBeanRegister.register(
              MBEAN_NAME + metricsListener.getName(), metricsListener)
      );
    }
  }

  private static void initializeMetricsEventListener() {
    ConfigManager configManager = ApolloInjector.getInstance(
        ConfigManager.class);
    DefaultApolloClientBootstrapArgsApi defaultApolloClientBootstrapArgsApi = new DefaultApolloClientBootstrapArgsApi(
        m_configUtil);
    DefaultApolloClientExceptionApi defaultApolloClientExceptionApi = new DefaultApolloClientExceptionApi(m_configUtil);
    DefaultApolloClientNamespaceApi defaultApolloClientNamespaceApi = new DefaultApolloClientNamespaceApi(
        configManager);
    DefaultApolloClientThreadPoolApi defaultApolloClientThreadPoolApi = new DefaultApolloClientThreadPoolApi(
        RemoteConfigRepository.m_executorService,
        AbstractConfig.m_executorService, AbstractConfigFile.m_executorService,
        AbstractApolloClientMetricsExporter.m_executorService);

    MONITOR_CONTEXT.setApolloClientBootstrapArgsMonitorApi(defaultApolloClientBootstrapArgsApi);
    MONITOR_CONTEXT.setApolloClientExceptionMonitorApi(defaultApolloClientExceptionApi);
    MONITOR_CONTEXT.setApolloClientNamespaceMonitorApi(defaultApolloClientNamespaceApi);
    MONITOR_CONTEXT.setApolloClientThreadPoolMonitorApi(defaultApolloClientThreadPoolApi);
    MONITOR_CONTEXT.setApolloClientMonitorEventListeners(
        Lists.newArrayList(defaultApolloClientBootstrapArgsApi,
            defaultApolloClientNamespaceApi, defaultApolloClientThreadPoolApi,
            defaultApolloClientExceptionApi));
  }

  private static void initializeMetricsExporter(
  ) {
    if (StringUtils.isEmpty(m_configUtil.getMonitorExternalType())) {
      return;
    }
    ApolloClientMetricsExporterFactory exporterFactory = ApolloInjector.getInstance(
            ApolloClientMetricsExporterFactory.class);
    ApolloClientMetricsExporter metricsReporter = exporterFactory.getMetricsReporter(
            MONITOR_CONTEXT.getApolloClientMonitorEventListeners());
    if (metricsReporter != null) {
      MONITOR_CONTEXT.setApolloClientMetricsExporter(metricsReporter);
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