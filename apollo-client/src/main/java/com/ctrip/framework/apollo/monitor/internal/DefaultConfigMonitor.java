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

import com.ctrip.framework.apollo.monitor.api.ApolloExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloRunningParamsMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.exporter.MetricsExporter;

/**
 * exposes all collected data through ConfigService
 *
 * @author Rawven
 */
public class DefaultConfigMonitor implements ConfigMonitor {

  private MetricsExporter reporter;
  private ApolloThreadPoolMonitorApi threadPoolMonitorApi = new NullThreadPoolMonitorApi();
  private ApolloExceptionMonitorApi exceptionMonitorApi = new NullExceptionMonitorApi();
  private ApolloNamespaceMonitorApi apolloNamespaceMonitorApi = new NullNamespaceMonitorApi();
  private ApolloRunningParamsMonitorApi apolloRunningParamsMonitorApi = new NullRunningParamsMonitorApi();

  @Override
  public ApolloThreadPoolMonitorApi getThreadPoolMonitorApi() {
    return threadPoolMonitorApi;
  }

  @Override
  public ApolloExceptionMonitorApi getExceptionMonitorApi() {
    return exceptionMonitorApi;
  }

  @Override
  public ApolloNamespaceMonitorApi getNamespaceMonitorApi() {
    return apolloNamespaceMonitorApi;
  }

  @Override
  public ApolloRunningParamsMonitorApi getRunningParamsMonitorApi() {
    return apolloRunningParamsMonitorApi;
  }

  @Override
  public String getDataWithCurrentMonitoringSystemFormat() {
    if (reporter == null) {
      return "No MonitoringSystem Use";
    }
    return reporter.response();
  }

  public void init(ApolloNamespaceMonitorApi apolloNamespaceMonitorApi,
      ApolloThreadPoolMonitorApi threadPoolMonitorApi,
      ApolloExceptionMonitorApi exceptionMonitorApi,
      ApolloRunningParamsMonitorApi apolloRunningParamsMonitorApi,
      MetricsExporter reporter) {
    this.apolloNamespaceMonitorApi = apolloNamespaceMonitorApi;
    this.threadPoolMonitorApi = threadPoolMonitorApi;
    this.exceptionMonitorApi = exceptionMonitorApi;
    this.apolloRunningParamsMonitorApi = apolloRunningParamsMonitorApi;
    this.reporter = reporter;
  }
}
