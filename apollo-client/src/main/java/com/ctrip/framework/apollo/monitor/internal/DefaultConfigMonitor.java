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

import com.ctrip.framework.apollo.monitor.api.ApolloClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.collector.impl.NullClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.impl.NullClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.impl.NullClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.impl.NullClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.exporter.MetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.impl.NullMetricsExporter;

/**
 * exposes all collected data through ConfigService
 *
 * @author Rawven
 */
public class DefaultConfigMonitor implements ConfigMonitor {

  private MetricsExporter reporter = new NullMetricsExporter();
  private ApolloClientThreadPoolMonitorApi threadPoolMonitorApi = new NullClientThreadPoolMonitorApi();
  private ApolloClientExceptionMonitorApi exceptionMonitorApi = new NullClientExceptionMonitorApi();
  private ApolloClientNamespaceMonitorApi apolloClientNamespaceMonitorApi = new NullClientNamespaceMonitorApi();
  private ApolloClientBootstrapArgsMonitorApi apolloClientBootstrapArgsMonitorApi = new NullClientBootstrapArgsMonitorApi();

  @Override
  public ApolloClientThreadPoolMonitorApi getThreadPoolMonitorApi() {
    return threadPoolMonitorApi;
  }

  @Override
  public ApolloClientExceptionMonitorApi getExceptionMonitorApi() {
    return exceptionMonitorApi;
  }

  @Override
  public ApolloClientNamespaceMonitorApi getNamespaceMonitorApi() {
    return apolloClientNamespaceMonitorApi;
  }

  @Override
  public ApolloClientBootstrapArgsMonitorApi getRunningParamsMonitorApi() {
    return apolloClientBootstrapArgsMonitorApi;
  }

  @Override
  public String getExporterData() {
    return reporter.response();
  }

  public void init(ApolloClientNamespaceMonitorApi apolloClientNamespaceMonitorApi,
      ApolloClientThreadPoolMonitorApi threadPoolMonitorApi,
      ApolloClientExceptionMonitorApi exceptionMonitorApi,
      ApolloClientBootstrapArgsMonitorApi apolloClientBootstrapArgsMonitorApi,
      MetricsExporter reporter) {
    this.apolloClientNamespaceMonitorApi = apolloClientNamespaceMonitorApi;
    this.threadPoolMonitorApi = threadPoolMonitorApi;
    this.exceptionMonitorApi = exceptionMonitorApi;
    this.apolloClientBootstrapArgsMonitorApi = apolloClientBootstrapArgsMonitorApi;
    this.reporter = reporter;
  }
}
