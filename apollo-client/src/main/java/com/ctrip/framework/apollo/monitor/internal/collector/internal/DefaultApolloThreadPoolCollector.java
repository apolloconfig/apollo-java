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
package com.ctrip.framework.apollo.monitor.internal.collector.internal;

import com.ctrip.framework.apollo.internals.AbstractConfig;
import com.ctrip.framework.apollo.internals.AbstractConfigFile;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.monitor.api.ApolloThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Rawven
 */
public class DefaultApolloThreadPoolCollector extends AbstractMetricsCollector implements
    ApolloThreadPoolMonitorApi {

  public static final String THREAD_POOL_METRICS = "ThreadPoolMetrics";
  public static final String[] THREAD_POOL_PARAMS = new String[]{"ThreadPoolName",
      "activeTaskCount", "queueSize",
      "completedTaskCount",
      "poolSize", "totalTaskCount", "corePoolSize", "maximumPoolSize", "largestPoolSize",
      "queueCapacity", "queueRemainingCapacity", "currentLoad"};

  private final ScheduledThreadPoolExecutor remoteConfigRepositoryExecutorService;
  private final ThreadPoolExecutor abstractConfigExecutorService;
  private final ThreadPoolExecutor abstractConfigFileExecutorService;

  public DefaultApolloThreadPoolCollector(
      ScheduledExecutorService remoteConfigRepositoryExecutorService,
      ExecutorService abstractConfigExecutorService,
      ExecutorService abstractConfigFileExecutorService) {
    super(THREAD_POOL_METRICS, "Nop");
    this.remoteConfigRepositoryExecutorService = (ScheduledThreadPoolExecutor) remoteConfigRepositoryExecutorService;
    this.abstractConfigExecutorService = (ThreadPoolExecutor) abstractConfigExecutorService;
    this.abstractConfigFileExecutorService = (ThreadPoolExecutor) abstractConfigFileExecutorService;
  }

  @Override
  public void collect0(MetricsEvent event) {
    // do nothing
    return;
  }

  @Override
  public void export0() {
    exportThreadPoolMetrics(abstractConfigExecutorService,
        AbstractConfig.class.getSimpleName());
    exportThreadPoolMetrics(abstractConfigFileExecutorService,
        AbstractConfigFile.class.getSimpleName());
    exportThreadPoolMetrics(remoteConfigRepositoryExecutorService,
        RemoteConfigRepository.class.getSimpleName());
  }

  @Override
  public boolean isSamplesUpdated() {
    // memory status special
    return true;
  }


  @SuppressWarnings("unchecked")
  public void exportThreadPoolMetrics(ThreadPoolExecutor executor,
      String name) {
    List<Double> metrics = Arrays.asList((double) executor.getActiveCount(),
        (double) executor.getQueue().size(),
        (double) executor.getCompletedTaskCount(), (double) executor.getPoolSize(),
        (double) executor.getTaskCount(), (double) executor.getCorePoolSize(),
        (double) executor.getMaximumPoolSize(), (double) executor.getLargestPoolSize(),
        (double) (executor.getQueue().remainingCapacity() + executor.getQueue().size()),
        (double) executor.getQueue().remainingCapacity(),
        (double) executor.getPoolSize() / executor.getMaximumPoolSize());
    for (int i = 0; i < metrics.size(); i++) {
      if (!gaugeSamples.containsKey(name + THREAD_POOL_PARAMS[i + 1])) {
        gaugeSamples.put(name + THREAD_POOL_PARAMS[i + 1],
            GaugeModel.builder().putTag(THREAD_POOL_PARAMS[0], name)
                .name(THREAD_POOL_PARAMS[i + 1])
                .value(0).apply(GaugeModel.DOUBLE_CONVERTER).build());
      }
      gaugeSamples.get(name + THREAD_POOL_PARAMS[i + 1]).updateValue(metrics.get(i));
    }
  }


  @Override
  public int getRemoteConfigRepositoryThreadPoolActiveCount() {
    return remoteConfigRepositoryExecutorService.getActiveCount();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolQueueSize() {
    return remoteConfigRepositoryExecutorService.getQueue().size();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolCorePoolSize() {
    return remoteConfigRepositoryExecutorService.getCorePoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolMaximumPoolSize() {
    return remoteConfigRepositoryExecutorService.getMaximumPoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolPoolSize() {
    return remoteConfigRepositoryExecutorService.getPoolSize();
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolTaskCount() {
    return remoteConfigRepositoryExecutorService.getTaskCount();
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolCompletedTaskCount() {
    return remoteConfigRepositoryExecutorService.getCompletedTaskCount();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolLargestPoolSize() {
    return remoteConfigRepositoryExecutorService.getLargestPoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolRemainingCapacity() {
    return remoteConfigRepositoryExecutorService.getQueue().remainingCapacity();
  }

  @Override
  public double getRemoteConfigRepositoryThreadPoolCurrentLoad() {
    return (double) remoteConfigRepositoryExecutorService.getPoolSize()
        / remoteConfigRepositoryExecutorService.getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolActiveCount() {
    return abstractConfigExecutorService.getActiveCount();
  }

  @Override
  public int getAbstractConfigThreadPoolQueueSize() {
    return abstractConfigExecutorService.getQueue().size();
  }

  @Override
  public int getAbstractConfigThreadPoolCorePoolSize() {
    return abstractConfigExecutorService.getCorePoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolMaximumPoolSize() {
    return abstractConfigExecutorService.getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolPoolSize() {
    return abstractConfigExecutorService.getPoolSize();
  }

  @Override
  public long getAbstractConfigThreadPoolTaskCount() {
    return abstractConfigExecutorService.getTaskCount();
  }

  @Override
  public long getAbstractConfigThreadPoolCompletedTaskCount() {
    return abstractConfigExecutorService.getCompletedTaskCount();
  }

  @Override
  public int getAbstractConfigThreadPoolLargestPoolSize() {
    return abstractConfigExecutorService.getLargestPoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolRemainingCapacity() {
    return abstractConfigExecutorService.getQueue().remainingCapacity();
  }

  @Override
  public double getAbstractConfigThreadPoolCurrentLoad() {
    return (double) abstractConfigExecutorService.getPoolSize()
        / abstractConfigExecutorService.getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolActiveCount() {
    return abstractConfigFileExecutorService.getActiveCount();
  }

  @Override
  public int getAbstractConfigFileThreadPoolQueueSize() {
    return abstractConfigFileExecutorService.getQueue().size();
  }

  @Override
  public int getAbstractConfigFileThreadPoolCorePoolSize() {
    return abstractConfigFileExecutorService.getCorePoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolMaximumPoolSize() {
    return abstractConfigFileExecutorService.getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolPoolSize() {
    return abstractConfigFileExecutorService.getPoolSize();
  }

  @Override
  public long getAbstractConfigFileThreadPoolTaskCount() {
    return abstractConfigFileExecutorService.getTaskCount();
  }

  @Override
  public long getAbstractConfigFileThreadPoolCompletedTaskCount() {
    return abstractConfigFileExecutorService.getCompletedTaskCount();
  }

  @Override
  public int getAbstractConfigFileThreadPoolLargestPoolSize() {
    return abstractConfigFileExecutorService.getLargestPoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolRemainingCapacity() {
    return abstractConfigFileExecutorService.getQueue().remainingCapacity();
  }

  @Override
  public double getAbstractConfigFileThreadPoolCurrentLoad() {
    return (double) abstractConfigFileExecutorService.getPoolSize()
        / abstractConfigFileExecutorService.getMaximumPoolSize();
  }

}