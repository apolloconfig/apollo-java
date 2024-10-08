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
package com.ctrip.framework.apollo.monitor.api;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Rawven
 */
public interface ApolloClientThreadPoolMonitorApi {

  /**
   * get thread pool info key "RemoteConfigRepository" ,"AbstractConfig","AbstractConfigFile";
   */
  Map<String, ApolloThreadPoolInfo> getThreadPoolInfo();

  /**
   * RemoteConfigRepository.m_executorService
   */
  ApolloThreadPoolInfo getRemoteConfigRepositoryThreadPoolInfo();

  /**
   * AbstractConfig.m_executorService
   */
  ApolloThreadPoolInfo getAbstractConfigThreadPoolInfo();

  /**
   * AbstractConfigFile.m_executorService
   */
  ApolloThreadPoolInfo getAbstractConfigFileThreadPoolInfo();

  /**
   * AbstractApolloClientMetricsExporter.m_executorService
   */
  ApolloThreadPoolInfo getMetricsExporterThreadPoolInfo();


  class ApolloThreadPoolInfo {

    private ThreadPoolExecutor executor;

    public ApolloThreadPoolInfo(ThreadPoolExecutor executor) {
      this.executor = executor;
    }

    public ApolloThreadPoolInfo() {
    }


    public int getActiveTaskCount() {
      return executor != null ? executor.getActiveCount() : 0;
    }

    public int getQueueSize() {
      return executor != null ? executor.getQueue().size() : 0;
    }

    public int getCorePoolSize() {
      return executor != null ? executor.getCorePoolSize() : 0;
    }

    public int getMaximumPoolSize() {
      return executor != null ? executor.getMaximumPoolSize() : 0;
    }

    public int getPoolSize() {
      return executor != null ? executor.getPoolSize() : 0;
    }

    public long getTotalTaskCount() {
      return executor != null ? executor.getTaskCount() : 0;
    }

    public long getCompletedTaskCount() {
      return executor != null ? executor.getCompletedTaskCount() : 0;
    }

    public int getLargestPoolSize() {
      return executor != null ? executor.getLargestPoolSize() : 0;
    }

    public int getQueueRemainingCapacity() {
      return executor != null ? executor.getQueue().remainingCapacity() : 0;
    }

  }
}
