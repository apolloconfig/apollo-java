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
package com.ctrip.framework.apollo.monitor.internal.exporter;

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.SampleModel;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public abstract class AbstractApolloClientMetricsExporter implements ApolloClientMetricsExporter {

  private static final Logger log = DeferredLoggerFactory.getLogger(
      AbstractApolloClientMetricsExporter.class);
  private static final ScheduledExecutorService m_executorService;
  private static final long INITIAL_DELAY = 5L;
  private static final int THREAD_POOL_SIZE = 1;

  static {
    m_executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
        ApolloThreadFactory.create("MetricsReporter", true));
  }

  protected List<ApolloClientMonitorEventListener> collectors;

  @Override
  public void init(List<ApolloClientMonitorEventListener> collectors, long collectPeriod) {
    log.info("Initializing metrics exporter with {} collectors and collect period of {} seconds.",
        collectors.size(), collectPeriod);
    doInit();
    this.collectors = collectors;
    initScheduleMetricsCollectSync(collectPeriod);
    log.info("Metrics collection scheduled with a period of {} seconds.", collectPeriod);
  }

  /**
   * Provide initialization extension points
   */
  protected abstract void doInit();

  private void initScheduleMetricsCollectSync(long collectPeriod) {
    m_executorService.scheduleAtFixedRate(() -> {
      try {
        updateMetricsData();
      } catch (Throwable ex) {
        log.error("Error updating metrics data", ex);
      }
    }, INITIAL_DELAY, collectPeriod, TimeUnit.SECONDS);
  }

  protected void updateMetricsData() {
    log.debug("Start to update metrics data job");
    collectors.forEach(collector -> {
      if (collector.isMetricsSampleUpdated()) {
        log.debug("Collector {} has updated samples.", collector.mBeanName());
        collector.export().forEach(this::registerSample);
      }
    });
  }

  protected void registerSample(SampleModel sample) {
    try {
      switch (sample.getType()) {
        case GAUGE:
          GaugeModel gaugeModel = (GaugeModel) sample;
          registerOrUpdateGaugeSample(gaugeModel.getName(), gaugeModel.getTags(),
              gaugeModel.getValue());
          break;
        case COUNTER:
          CounterModel counterModel = (CounterModel) sample;
          registerOrUpdateCounterSample(counterModel.getName(), counterModel.getTags(),
              counterModel.getIncreaseValueAndResetZero());
          break;
        default:
          log.warn("Unsupported sample type: {}", sample.getType());
          break;
      }
    } catch (Exception e) {
      log.error("Register sample error", e);
    }
  }

}
