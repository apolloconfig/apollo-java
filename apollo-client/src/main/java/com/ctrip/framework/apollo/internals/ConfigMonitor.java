package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.metrics.collector.ConfigManagerCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.StartUpCollector;
import com.ctrip.framework.apollo.metrics.collector.TracerCollector;
import com.ctrip.framework.apollo.metrics.reporter.MetricsReporter;
import com.ctrip.framework.apollo.metrics.util.JMXUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigMonitor implements ConfigMonitorMBean {
    public static final String JmxName = "com.ctrip.framework.apollo.metrics:type=ConfigMonitor";
    private MetricsReporter reporter;
    private DefaultConfigManager configManager;
    private StartUpCollector startUpCollector;
    private TracerCollector tracerCollector;
    private ConfigManagerCollector configManagerCollector;
    private List<MetricsCollector> collectors = new ArrayList<>();

    public ConfigMonitor() {
        initCollectors();
        configManager = (DefaultConfigManager) ApolloInjector.getInstance(ConfigManager.class);

        ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        String protocol = configUtil.getMonitorProtocol();
        if (Objects.equals(protocol, JMXUtil.JMX)) {
            JMXUtil.register(JmxName, this);
        } else if (protocol != null) {
            reporter = ServiceBootstrap.loadPrimary(MetricsReporter.class);
            reporter.init(collectors);
        }
    }

    public void initCollectors() {
        startUpCollector = new StartUpCollector();
        tracerCollector = new TracerCollector();
        configManagerCollector = new ConfigManagerCollector(configManager.m_configs,
            configManager.m_configLocks, configManager.m_configFiles,
            configManager.m_configFileLocks);
        collectors.add(startUpCollector);
        collectors.add(tracerCollector);
        collectors.add(configManagerCollector);
    }

    public List<MetricsCollector> getCollectors() {
        return collectors;
    }

    @Override
    public String getAppId() {
        return startUpCollector.getAppId();
    }

    @Override
    public String getCluster() {
        return startUpCollector.getCluster();
    }

    @Override
    public String getEnv() {
        return startUpCollector.getApolloEnv();
    }

    @Override
    public String getNamespace404() {
        return tracerCollector.getNamespace404();
    }

    @Override
    public String getNamespaceTimeout() {
        return tracerCollector.getNamespaceTimeout();
    }

    @Override
    public int getExceptionNum() {
        return tracerCollector.getExceptionNum();
    }

    @Override
    public String getNamespaceUsed() {
        return configManagerCollector.getNamespaceUsed().toString();
    }

    @Override
    public String getDataWithCurrentMonitoringSystemFormat() {
        if (reporter == null) {
            return "No MonitoringSystem Use";
        }
        return reporter.response();
    }
}
