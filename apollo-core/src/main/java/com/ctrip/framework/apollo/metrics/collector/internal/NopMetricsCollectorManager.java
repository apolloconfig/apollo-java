package com.ctrip.framework.apollo.metrics.collector.internal;

import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollectorManager;
import java.util.ArrayList;
import java.util.List;

public class NopMetricsCollectorManager implements MetricsCollectorManager {
    @Override
    public List<MetricsCollector> getCollectors() {
        return new ArrayList<>();
    }
}
