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

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.monitor.api.ApolloClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;

/**
 * exposes all collected data through ConfigService
 *
 * @author Rawven
 */
public class DefaultConfigMonitor implements ConfigMonitor {

  private ApolloClientMonitorContext apolloClientMonitorContext = ApolloInjector.getInstance(
      ApolloClientMonitorContext.class);

  @Override
  public ApolloClientThreadPoolMonitorApi getThreadPoolMonitorApi() {
    return apolloClientMonitorContext.getThreadPoolApi();
  }

  @Override
  public ApolloClientExceptionMonitorApi getExceptionMonitorApi() {
    return apolloClientMonitorContext.getExceptionApi();
  }

  @Override
  public ApolloClientNamespaceMonitorApi getNamespaceMonitorApi() {
    return apolloClientMonitorContext.getNamespaceApi();
  }

  @Override
  public ApolloClientBootstrapArgsMonitorApi getRunningParamsMonitorApi() {
    return apolloClientMonitorContext.getBootstrapArgsApi();
  }

  @Override
  public String getExporterData() {
    return apolloClientMonitorContext.getMetricsExporter().response();
  }
}
