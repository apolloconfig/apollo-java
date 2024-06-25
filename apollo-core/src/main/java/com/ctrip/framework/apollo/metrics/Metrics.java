package com.ctrip.framework.apollo.metrics;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollectorManager;
import com.ctrip.framework.apollo.metrics.collector.internal.NopMetricsCollectorManager;
import com.ctrip.framework.foundation.Foundation;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.List;

public abstract class Metrics {

    private static MetricsCollectorManager collectorManager;

    static {
        List<MetricsCollectorManager> managers = ServiceBootstrap.loadAllOrdered(MetricsCollectorManager.class);
        if (!managers.isEmpty()) {
            collectorManager = managers.get(0);
        } else {
            collectorManager = new NopMetricsCollectorManager();
        }
    }

    public static void push(MetricsEvent event) {
        for (MetricsCollector collector : collectorManager.getCollectors()) {
            if (collector.isSupport(event.getTag())) {
                collector.collect(event);
                return;
            }
        }
    }
    public static void push(String tag,Object...data){

    }

    public static boolean isMetricsEnabled() {
        // 1. Get app.id from System Property
        String enabled = System.getProperty(ApolloClientSystemConsts.APOLLO_MONITOR_ENABLED);
        if (Boolean.parseBoolean(enabled)) {
            return true;
        }
        enabled = Foundation.app().getProperty(ApolloClientSystemConsts.APOLLO_MONITOR_ENABLED, "false");
        return Boolean.parseBoolean(enabled);
    }
}
