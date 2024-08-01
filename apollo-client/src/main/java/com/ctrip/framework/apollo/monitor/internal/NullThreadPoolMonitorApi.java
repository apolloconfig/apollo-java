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
package com.ctrip.framework.apollo.monitor.internal;

import com.ctrip.framework.apollo.monitor.api.ApolloThreadPoolMonitorApi;

public class NullThreadPoolMonitorApi implements ApolloThreadPoolMonitorApi {

  @Override
  public int getRemoteConfigRepositoryThreadPoolActiveCount() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolQueueSize() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolCorePoolSize() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolMaximumPoolSize() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolPoolSize() {
    return 0;
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolTaskCount() {
    return 0;
  }

  @Override
  public long getRemoteConfigRepositoryThreadPoolCompletedTaskCount() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolLargestPoolSize() {
    return 0;
  }

  @Override
  public int getRemoteConfigRepositoryThreadPoolRemainingCapacity() {
    return 0;
  }

  @Override
  public double getRemoteConfigRepositoryThreadPoolCurrentLoad() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolActiveCount() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolQueueSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolCorePoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolMaximumPoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolPoolSize() {
    return 0;
  }

  @Override
  public long getAbstractConfigThreadPoolTaskCount() {
    return 0;
  }

  @Override
  public long getAbstractConfigThreadPoolCompletedTaskCount() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolLargestPoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigThreadPoolRemainingCapacity() {
    return 0;
  }

  @Override
  public double getAbstractConfigThreadPoolCurrentLoad() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolActiveCount() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolQueueSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolCorePoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolMaximumPoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolPoolSize() {
    return 0;
  }

  @Override
  public long getAbstractConfigFileThreadPoolTaskCount() {
    return 0;
  }

  @Override
  public long getAbstractConfigFileThreadPoolCompletedTaskCount() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolLargestPoolSize() {
    return 0;
  }

  @Override
  public int getAbstractConfigFileThreadPoolRemainingCapacity() {
    return 0;
  }

  @Override
  public double getAbstractConfigFileThreadPoolCurrentLoad() {
    return 0;
  }
}
