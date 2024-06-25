package com.ctrip.framework.apollo.metrics;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.internals.ConfigMonitor;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollectorManager;
import java.util.List;

public class DefaultMetricsCollectorManager implements MetricsCollectorManager {
    private final ConfigMonitor configMonitor;

    public DefaultMetricsCollectorManager() {
        configMonitor = ApolloInjector.getInstance(ConfigMonitor.class);
    }

    @Override
    public List<MetricsCollector> getCollectors() {
        return configMonitor.getCollectors();
    }

}
