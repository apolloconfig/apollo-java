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

import javax.management.MXBean;

/**
 * @author Rawven
 */
@MXBean
public interface ApolloThreadPoolMonitorApi {


  int getRemoteConfigRepositoryThreadPoolActiveCount();

  int getRemoteConfigRepositoryThreadPoolQueueSize();

  int getRemoteConfigRepositoryThreadPoolCorePoolSize();

  int getRemoteConfigRepositoryThreadPoolMaximumPoolSize();

  int getRemoteConfigRepositoryThreadPoolPoolSize();

  long getRemoteConfigRepositoryThreadPoolTaskCount();

  long getRemoteConfigRepositoryThreadPoolCompletedTaskCount();

  int getRemoteConfigRepositoryThreadPoolLargestPoolSize();

  int getRemoteConfigRepositoryThreadPoolRemainingCapacity();

  double getRemoteConfigRepositoryThreadPoolCurrentLoad();

  int getAbstractConfigThreadPoolActiveCount();

  int getAbstractConfigThreadPoolQueueSize();

  int getAbstractConfigThreadPoolCorePoolSize();

  int getAbstractConfigThreadPoolMaximumPoolSize();

  int getAbstractConfigThreadPoolPoolSize();

  long getAbstractConfigThreadPoolTaskCount();

  long getAbstractConfigThreadPoolCompletedTaskCount();

  int getAbstractConfigThreadPoolLargestPoolSize();

  int getAbstractConfigThreadPoolRemainingCapacity();

  double getAbstractConfigThreadPoolCurrentLoad();


  int getAbstractConfigFileThreadPoolActiveCount();

  int getAbstractConfigFileThreadPoolQueueSize();

  int getAbstractConfigFileThreadPoolCorePoolSize();

  int getAbstractConfigFileThreadPoolMaximumPoolSize();

  int getAbstractConfigFileThreadPoolPoolSize();

  long getAbstractConfigFileThreadPoolTaskCount();

  long getAbstractConfigFileThreadPoolCompletedTaskCount();

  int getAbstractConfigFileThreadPoolLargestPoolSize();

  int getAbstractConfigFileThreadPoolRemainingCapacity();

  double getAbstractConfigFileThreadPoolCurrentLoad();

}
