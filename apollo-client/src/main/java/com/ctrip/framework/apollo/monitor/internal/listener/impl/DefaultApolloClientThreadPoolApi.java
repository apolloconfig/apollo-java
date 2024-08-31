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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;

import com.ctrip.framework.apollo.internals.AbstractConfig;
import com.ctrip.framework.apollo.internals.AbstractConfigFile;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.exporter.AbstractApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxThreadPoolMBean;
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMonitorEventListener;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Rawven
 */
public class DefaultApolloClientThreadPoolApi extends
    AbstractApolloClientMonitorEventListener implements
    ApolloClientThreadPoolMonitorApi, ApolloClientJmxThreadPoolMBean {

  public static final String REMOTE_CONFIG_REPOSITORY = RemoteConfigRepository.class.getSimpleName();
  public static final String ABSTRACT_CONFIG = AbstractConfig.class.getSimpleName();
  public static final String ABSTRACT_CONFIG_FILE = AbstractConfigFile.class.getSimpleName();
  public static final String METRICS_EXPORTER = AbstractApolloClientMetricsExporter.class.getSimpleName();
  private final Map<String, ApolloThreadPoolInfo> executorMap = Maps.newHashMap();

  public DefaultApolloClientThreadPoolApi(
      ExecutorService remoteConfigRepositoryExecutorService,
      ExecutorService abstractConfigExecutorService,
      ExecutorService abstractConfigFileExecutorService,
      ExecutorService metricsExporterExecutorService) {
    super(TAG_THREAD_POOL);
    executorMap.put(REMOTE_CONFIG_REPOSITORY,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) remoteConfigRepositoryExecutorService));
    executorMap.put(ABSTRACT_CONFIG,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) abstractConfigExecutorService));
    executorMap.put(ABSTRACT_CONFIG_FILE,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) abstractConfigFileExecutorService));
    executorMap.put(METRICS_EXPORTER,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) metricsExporterExecutorService));
  }

  @Override
  public void export0() {
    executorMap.forEach((key, value) -> exportThreadPoolMetrics(value, key));
  }

  private void exportThreadPoolMetrics(ApolloThreadPoolInfo info, String threadPoolName) {

    createOrUpdateGaugeSample(METRICS_THREAD_POOL_ACTIVE_TASK_COUNT,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getActiveTaskCount());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_QUEUE_SIZE,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getQueueSize());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_COMPLETED_TASK_COUNT,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        (double) info.getCompletedTaskCount());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_POOL_SIZE, new String[]{METRICS_THREAD_POOL_NAME},
        new String[]{threadPoolName}, info.getPoolSize());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_TOTAL_TASK_COUNT,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        (double) info.getTotalTaskCount());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_CORE_POOL_SIZE,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getCorePoolSize());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_MAXIMUM_POOL_SIZE,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getMaximumPoolSize());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_LARGEST_POOL_SIZE,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getLargestPoolSize());
    createOrUpdateGaugeSample(METRICS_THREAD_POOL_QUEUE_REMAINING_CAPACITY,
        new String[]{METRICS_THREAD_POOL_NAME}, new String[]{threadPoolName},
        info.getQueueRemainingCapacity());
  }


  @Override
  public boolean isMetricsSampleUpdated() {
    // memory status special
    return true;
  }

  @Override
  public Map<String, ApolloThreadPoolInfo> getThreadPoolInfo() {
    return executorMap;
  }

  @Override
  public ApolloThreadPoolInfo getRemoteConfigRepositoryThreadPoolInfo() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY);
  }

  @Override
  public ApolloThreadPoolInfo getAbstractConfigThreadPoolInfo() {
    return executorMap.get(ABSTRACT_CONFIG);
  }

  @Override
  public ApolloThreadPoolInfo getAbstractConfigFileThreadPoolInfo() {
    return executorMap.get(ABSTRACT_CONFIG_FILE);
  }

  @Override
  public ApolloThreadPoolInfo getMetricsExporterThreadPoolInfo() {
    return executorMap.get(METRICS_EXPORTER);
  }
}