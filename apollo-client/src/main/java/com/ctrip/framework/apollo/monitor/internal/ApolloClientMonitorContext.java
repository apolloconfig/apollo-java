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
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.impl.NullApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientThreadPoolMonitorApi;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * @author Rawven
 */
public class ApolloClientMonitorContext {

  private ApolloClientExceptionMonitorApi apolloClientExceptionMonitorApi = new NullClientExceptionMonitorApi();
  private ApolloClientNamespaceMonitorApi apolloClientNamespaceMonitorApi = new NullClientNamespaceMonitorApi();
  private ApolloClientBootstrapArgsMonitorApi apolloClientBootstrapArgsMonitorApi = new NullClientBootstrapArgsMonitorApi();
  private ApolloClientThreadPoolMonitorApi apolloClientThreadPoolMonitorApi = new NullClientThreadPoolMonitorApi();
  private ApolloClientMetricsExporter apolloClientMetricsExporter = new NullApolloClientMetricsExporter();

  public void setApolloClientExceptionMonitorApi(
      ApolloClientExceptionMonitorApi apolloClientExceptionMonitorApi) {
    this.apolloClientExceptionMonitorApi = apolloClientExceptionMonitorApi;
  }

  public void setApolloClientNamespaceMonitorApi(
      ApolloClientNamespaceMonitorApi apolloClientNamespaceMonitorApi) {
    this.apolloClientNamespaceMonitorApi = apolloClientNamespaceMonitorApi;
  }

  public void setApolloClientBootstrapArgsMonitorApi(
      ApolloClientBootstrapArgsMonitorApi apolloClientBootstrapArgsMonitorApi) {
    this.apolloClientBootstrapArgsMonitorApi = apolloClientBootstrapArgsMonitorApi;
  }

  public void setApolloClientThreadPoolMonitorApi(
      ApolloClientThreadPoolMonitorApi apolloClientThreadPoolMonitorApi) {
    this.apolloClientThreadPoolMonitorApi = apolloClientThreadPoolMonitorApi;
  }

  public void setApolloClientMetricsExporter(
      ApolloClientMetricsExporter apolloClientMetricsExporter) {
    this.apolloClientMetricsExporter = apolloClientMetricsExporter;
  }

  public List<ApolloClientMonitorEventListener> getCollectors() {
    return Lists.newArrayList(
        (ApolloClientMonitorEventListener) apolloClientBootstrapArgsMonitorApi,
        (ApolloClientMonitorEventListener) apolloClientThreadPoolMonitorApi,
        (ApolloClientMonitorEventListener) apolloClientExceptionMonitorApi,
        (ApolloClientMonitorEventListener) apolloClientNamespaceMonitorApi);
  }

  public ApolloClientExceptionMonitorApi getExceptionApi() {
    return apolloClientExceptionMonitorApi;
  }

  public ApolloClientNamespaceMonitorApi getNamespaceApi() {
    return apolloClientNamespaceMonitorApi;
  }

  public ApolloClientBootstrapArgsMonitorApi getBootstrapArgsApi() {
    return apolloClientBootstrapArgsMonitorApi;
  }

  public ApolloClientThreadPoolMonitorApi getThreadPoolApi() {
    return apolloClientThreadPoolMonitorApi;
  }

  public ApolloClientMetricsExporter getMetricsExporter() {
    return apolloClientMetricsExporter;
  }
}
