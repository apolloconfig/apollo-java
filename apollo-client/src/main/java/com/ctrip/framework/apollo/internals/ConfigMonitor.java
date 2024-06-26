package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.metrics.collector.ConfigCollector;
import com.ctrip.framework.apollo.metrics.collector.StartUpCollector;
import com.ctrip.framework.apollo.metrics.collector.TracerCollector;
import com.ctrip.framework.apollo.metrics.reporter.MetricsReporter;

/**
 * exposes all collected data through ConfigService
 * @author Rawven
 */
public class ConfigMonitor implements ConfigMonitorMBean {
    private MetricsReporter reporter;
    private StartUpCollector startUpCollector;
    private TracerCollector tracerCollector;
    private ConfigCollector configCollector;

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
        return configCollector.getNamespaceUsed().toString();
    }

    @Override
    public String getNamespaceUsedTime() {
        return configCollector.getAllNamespaceUsedTimes();
    }

    @Override
    public String getDataWithCurrentMonitoringSystemFormat() {
        if (reporter == null) {
            return "No MonitoringSystem Use";
        }
        return reporter.response();
    }

    public void setConfigCollector(ConfigCollector configCollector) {
        this.configCollector = configCollector;
    }

    public void setStartUpCollector(StartUpCollector startUpCollector) {
        this.startUpCollector = startUpCollector;
    }

    public void setTracerCollector(TracerCollector tracerCollector) {
        this.tracerCollector = tracerCollector;
    }

    public void setMetricsReporter(MetricsReporter reporter) {
        this.reporter = reporter;
    }
}
