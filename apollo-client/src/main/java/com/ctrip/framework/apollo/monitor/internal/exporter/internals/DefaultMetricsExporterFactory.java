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
package com.ctrip.framework.apollo.monitor.internal.exporter.internals;

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.MBEAN_NAME;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.monitor.internal.collector.MetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.collector.internal.DefaultMetricsCollectorManager;
import com.ctrip.framework.apollo.monitor.internal.exporter.MetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.MetricsExporterFactory;
import com.ctrip.framework.apollo.monitor.internal.util.JMXUtil;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.List;
import org.slf4j.Logger;

public class DefaultMetricsExporterFactory implements MetricsExporterFactory {

  private static final Logger logger = DeferredLoggerFactory.getLogger(
      DefaultMetricsCollectorManager.class);
  private final ConfigUtil m_configUtil;

  public DefaultMetricsExporterFactory() {
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public MetricsExporter getMetricsReporter(List<MetricsCollector> collectors) {
    //initialize reporter
    if (m_configUtil.isClientMonitorJmxEnabled()) {
      collectors.forEach(metricsCollector ->
          JMXUtil.register(MBEAN_NAME + metricsCollector.name(),
              metricsCollector));
    }
    String externalSystemType = m_configUtil.getMonitorExternalType();
    MetricsExporter reporter = null;
    if (externalSystemType != null) {
      List<MetricsExporter> metricsExporters = ServiceBootstrap.loadAllOrdered(
          MetricsExporter.class);
      for (MetricsExporter metricsExporter : metricsExporters) {
        if (metricsExporter.isSupport(externalSystemType)) {
          reporter = metricsExporter;
          reporter.init(collectors, m_configUtil.getMonitorExternalExportPeriod());
          break;
        }
      }
      if (reporter == null) {
        String errorMessage =
            "No matching exporter found with monitor-external-type " + externalSystemType;
        ApolloConfigException exception = new ApolloConfigException(errorMessage);
        logger.error(
            "Error initializing exporter for external-type: {}. Please check if external-type is misspelled or the correct dependency is not introduced, such as apollo-plugin-client-prometheus",
            externalSystemType, exception);
        Tracer.logError(exception);
      }
    }
    return reporter;
  }
}
