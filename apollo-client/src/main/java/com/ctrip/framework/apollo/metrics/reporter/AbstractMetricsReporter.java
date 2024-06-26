package com.ctrip.framework.apollo.metrics.reporter;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.internals.DefaultMetricsCollectorManager;
import com.ctrip.framework.apollo.metrics.collector.MetricsCollector;
import com.ctrip.framework.apollo.metrics.model.CounterMetricsSample;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * General framework for access monitoring systems
 *
 * @author Rawven
 */
@SuppressWarnings("all")
public abstract class AbstractMetricsReporter implements MetricsReporter {

  private static final Logger log = DeferredLoggerFactory.getLogger(
      DefaultMetricsCollectorManager.class);
  private static ScheduledExecutorService m_executorService;
  private List<MetricsCollector> collectors;
  private ConfigUtil m_configUtil;

  @Override
  public void init(List<MetricsCollector> collectors) {
    //...
    doInit();
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    this.collectors = collectors;
    initScheduleMetricsCollectSync();
  }

  protected abstract void doInit();

  private void initScheduleMetricsCollectSync() {
    m_executorService = Executors.newScheduledThreadPool(1);
    m_executorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          log.info("Start to update metrics data job");
          updateMetricsData();
        } catch (Throwable ex) {
          //ignore
        }
      }
    }, 1, m_configUtil.getMonitorCollectPeriod(), TimeUnit.SECONDS);
  }

  private void updateMetricsData() {
    for (MetricsCollector collector : collectors) {
      if (!collector.isSamplesUpdated()) {
        continue;
      }
      List<MetricsSample> export = collector.export();
      for (MetricsSample metricsSample : export) {
        registerSample(metricsSample);
      }
    }
  }

  private void registerSample(MetricsSample sample) {
    try {
      switch (sample.getType()) {
        case GAUGE:
          registerGaugeSample((GaugeMetricsSample<?>) sample);
          break;
        case COUNTER:
          registerCounterSample((CounterMetricsSample) sample);
          break;
        default:
          log.warn("UnSupport sample type: {}", sample.getType());
          break;
      }
    } catch (Exception e) {
      log.error("Register sample error", e);
    }
  }

  protected String[][] getTags(MetricsSample sample) {
    Map<String, String> tags = sample.getTags();
    if (tags == null || tags.isEmpty()) {
      return new String[][]{new String[0], new String[0]};
    }
    String[] labelNames = tags.keySet().toArray(new String[0]);
    String[] labelValues = tags.values().toArray(new String[0]);
    return new String[][]{labelNames, labelValues};
  }

}
