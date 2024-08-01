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
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

  private void exportThreadPoolMetrics(ApolloThreadPoolInfo info, String name) {
    List<Double> metrics = Arrays.asList(
        (double) info.getActiveTaskCount(),
        (double) info.getQueueSize(),
        (double) info.getCompletedTaskCount(),
        (double) info.getPoolSize(),
        (double) info.getTotalTaskCount(),
        (double) info.getCorePoolSize(),
        (double) info.getMaximumPoolSize(),
        (double) info.getLargestPoolSize(),
        (double) info.getQueueCapacity(),
        (double) info.getQueueRemainingCapacity(),
        info.getCurrentLoad()
    );

    for (int i = 0; i < metrics.size(); i++) {
      String key = name + METRICS_THREAD_POOL_PARAMS[i + 1];
      createOrUpdateGaugeSample(key, METRICS_THREAD_POOL_PARAMS[i + 1],
          Collections.singletonMap(METRICS_THREAD_POOL_PARAMS[0], name), metrics.get(i));
    }
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

  public static class ApolloThreadPoolInfo {

    private final ThreadPoolExecutor executor;

    public ApolloThreadPoolInfo(ThreadPoolExecutor executor) {
      this.executor = executor;
    }

    public int getActiveTaskCount() {
      return executor.getActiveCount();
    }

    public int getQueueSize() {
      return executor.getQueue().size();
    }

    public int getCorePoolSize() {
      return executor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
      return executor.getMaximumPoolSize();
    }

    public int getPoolSize() {
      return executor.getPoolSize();
    }

    public long getTotalTaskCount() {
      return executor.getTaskCount();
    }

    public long getCompletedTaskCount() {
      return executor.getCompletedTaskCount();
    }

    public int getLargestPoolSize() {
      return executor.getLargestPoolSize();
    }

    public int getQueueCapacity() {
      return executor.getQueue().remainingCapacity() + executor.getQueue().size();
    }

    public int getQueueRemainingCapacity() {
      return executor.getQueue().remainingCapacity();
    }

    public double getCurrentLoad() {
      return (double) executor.getPoolSize() / executor.getMaximumPoolSize();
    }
  }
}