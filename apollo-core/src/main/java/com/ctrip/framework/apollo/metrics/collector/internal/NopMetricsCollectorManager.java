package com.ctrip.framework.apollo.metrics.collector.internal;

import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollectorManager;
import java.util.List;

/**
 * @author Rawven
 */
public class NopMetricsCollectorManager implements MetricsCollectorManager {

  @Override
  public List<MetricsCollector> getCollectors() {
    return null;
  }
}
