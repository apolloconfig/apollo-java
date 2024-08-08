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
package com.ctrip.framework.apollo.monitor.internal.collector.impl;

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.*;

import com.ctrip.framework.apollo.internals.AbstractConfig;
import com.ctrip.framework.apollo.internals.AbstractConfigFile;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.jmx.mbean.ApolloClientJmxThreadPoolMBean;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloConfigMetricsEvent;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Rawven
 */
public class DefaultApolloClientThreadPoolApi extends AbstractMetricsCollector implements
    ApolloClientThreadPoolMonitorApi, ApolloClientJmxThreadPoolMBean {

  public static final String REMOTE_CONFIG_REPOSITORY = RemoteConfigRepository.class.getSimpleName();
  public static final String ABSTRACT_CONFIG = AbstractConfig.class.getSimpleName();
  public static final String ABSTRACT_CONFIG_FILE = AbstractConfigFile.class.getSimpleName();

  private final Map<String, ApolloThreadPoolInfo> executorMap = Maps.newHashMap();

  public DefaultApolloClientThreadPoolApi(
      ExecutorService remoteConfigRepositoryExecutorService,
      ExecutorService abstractConfigExecutorService,
      ExecutorService abstractConfigFileExecutorService) {
    super(TAG_THREAD_POOL);
    executorMap.put(REMOTE_CONFIG_REPOSITORY,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) remoteConfigRepositoryExecutorService));
    executorMap.put(ABSTRACT_CONFIG,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) abstractConfigExecutorService));
    executorMap.put(ABSTRACT_CONFIG_FILE,
        new ApolloThreadPoolInfo((ThreadPoolExecutor) abstractConfigFileExecutorService));
  }

  @Override
  public void collect0(ApolloConfigMetricsEvent event) {
    // do nothing
  }

  @Override
  public void export0() {
    for (Map.Entry<String, ApolloThreadPoolInfo> entry : executorMap.entrySet()) {
      exportThreadPoolMetrics(entry.getValue(), entry.getKey());
    }
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
      if (!gaugeSamples.containsKey(key)) {
        gaugeSamples.put(key,
            (GaugeModel) GaugeModel.create(METRICS_THREAD_POOL_PARAMS[i + 1], 0)
                .putTag(METRICS_THREAD_POOL_PARAMS[0], name));
      }
      gaugeSamples.get(key).updateValue(metrics.get(i));
    }
  }


  @Override
  public boolean isSamplesUpdated() {
    // memory status special
    return true;
  }


  @Override
  public int getRemoteConfigRepositoryThreadPoolActiveCount() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getActiveTaskCount();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolQueueSize() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getQueueSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolCorePoolSize() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getCorePoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolMaximumPoolSize() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getMaximumPoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolPoolSize() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getPoolSize();
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolTaskCount() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getTotalTaskCount();
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolCompletedTaskCount() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getCompletedTaskCount();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolLargestPoolSize() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getLargestPoolSize();
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolRemainingCapacity() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getQueueRemainingCapacity();
  }

  @Override
  public double getRemoteConfigRepositoryThreadPoolCurrentLoad() {
    return executorMap.get(REMOTE_CONFIG_REPOSITORY).getCurrentLoad();
  }

  @Override
  public int getAbstractConfigThreadPoolActiveCount() {
    return executorMap.get(ABSTRACT_CONFIG).getActiveTaskCount();
  }

  @Override
  public int getAbstractConfigThreadPoolQueueSize() {
    return executorMap.get(ABSTRACT_CONFIG).getQueueSize();
  }

  @Override
  public int getAbstractConfigThreadPoolCorePoolSize() {
    return executorMap.get(ABSTRACT_CONFIG).getCorePoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolMaximumPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG).getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG).getPoolSize();
  }

  @Override
  public long getAbstractConfigThreadPoolTaskCount() {
    return executorMap.get(ABSTRACT_CONFIG).getTotalTaskCount();
  }

  @Override
  public long getAbstractConfigThreadPoolCompletedTaskCount() {
    return executorMap.get(ABSTRACT_CONFIG).getCompletedTaskCount();
  }

  @Override
  public int getAbstractConfigThreadPoolLargestPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG).getLargestPoolSize();
  }

  @Override
  public int getAbstractConfigThreadPoolRemainingCapacity() {
    return executorMap.get(ABSTRACT_CONFIG).getQueueRemainingCapacity();
  }

  @Override
  public double getAbstractConfigThreadPoolCurrentLoad() {
    return executorMap.get(ABSTRACT_CONFIG).getCurrentLoad();
  }

  @Override
  public int getAbstractConfigFileThreadPoolActiveCount() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getActiveTaskCount();
  }

  @Override
  public int getAbstractConfigFileThreadPoolQueueSize() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getQueueSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolCorePoolSize() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getCorePoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolMaximumPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getMaximumPoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getPoolSize();
  }

  @Override
  public long getAbstractConfigFileThreadPoolTaskCount() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getTotalTaskCount();
  }

  @Override
  public long getAbstractConfigFileThreadPoolCompletedTaskCount() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getCompletedTaskCount();
  }

  @Override
  public int getAbstractConfigFileThreadPoolLargestPoolSize() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getLargestPoolSize();
  }

  @Override
  public int getAbstractConfigFileThreadPoolRemainingCapacity() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getQueueRemainingCapacity();
  }

  @Override
  public double getAbstractConfigFileThreadPoolCurrentLoad() {
    return executorMap.get(ABSTRACT_CONFIG_FILE).getCurrentLoad();

  }

  @Override
  public Map<String, ApolloThreadPoolInfo> getThreadPoolInfo() {
    return executorMap;
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