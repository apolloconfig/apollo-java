package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.metrics.collector.ConfigCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollectorManager;
import com.ctrip.framework.apollo.metrics.collector.StartUpCollector;
import com.ctrip.framework.apollo.metrics.collector.TracerCollector;
import com.ctrip.framework.apollo.metrics.reporter.MetricsReporter;
import com.ctrip.framework.apollo.metrics.util.JMXUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class DefaultMetricsCollectorManager implements MetricsCollectorManager {
    private static final Logger logger = DeferredLoggerFactory.getLogger(DefaultMetricsCollectorManager.class);
    private List<MetricsCollector> collectors;

    public DefaultMetricsCollectorManager() {
        ConfigMonitor configMonitor = ApolloInjector.getInstance(ConfigMonitor.class);
        ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        DefaultConfigManager configManager = (DefaultConfigManager) ApolloInjector.getInstance(ConfigManager.class);
        initialize(configMonitor, configUtil, configManager);
    }

    private void initialize(ConfigMonitor configMonitor, ConfigUtil configUtil,
        DefaultConfigManager configManager) {
        TracerCollector traceCollector = new TracerCollector();
        ConfigCollector configCollector = new ConfigCollector(configManager.m_configs,
            configManager.m_configLocks, configManager.m_configFiles, configManager.m_configFileLocks);
        StartUpCollector startUpCollector = new StartUpCollector(configUtil);

        configMonitor.setConfigCollector(configCollector);
        configMonitor.setStartUpCollector(startUpCollector);
        configMonitor.setTracerCollector(traceCollector);

        collectors = Lists.newArrayList(traceCollector, configCollector, startUpCollector);

        String protocol = configUtil.getMonitorProtocol();
        if (Objects.equals(protocol, JMXUtil.JMX)) {
            JMXUtil.register(JMXUtil.MBEAN_NAME, configMonitor);
        } else if (protocol != null) {
            try {
                MetricsReporter reporter = ServiceBootstrap.loadPrimary(MetricsReporter.class);
                if (reporter != null) {
                    reporter.init(collectors);
                    configMonitor.setMetricsReporter(reporter);
                } else {
                    logger.warn("No MetricsReporter found for protocol: {}", protocol);
                }
            } catch (Exception e) {
                logger.error("Error initializing MetricsReporter for protocol: {}", protocol, e);
            }
        }
    }

    @Override
    public List<MetricsCollector> getCollectors() {
        return collectors;
    }

}
