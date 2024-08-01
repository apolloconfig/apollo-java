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

import com.ctrip.framework.apollo.monitor.api.ApolloRunningParamsMonitorApi;

public class NullRunningParamsMonitorApi implements ApolloRunningParamsMonitorApi {

  @Override
  public String getStartupParams(String key) {
    return "";
  }

  @Override
  public String getConfigServiceUrl() {
    return "";
  }

  @Override
  public String getAccessKeySecret() {
    return "";
  }

  @Override
  public Boolean getAutoUpdateInjectedSpringProperties() {
    return null;
  }

  @Override
  public Boolean getBootstrapEnabled() {
    return null;
  }

  @Override
  public String getBootstrapNamespaces() {
    return "";
  }

  @Override
  public Boolean getBootstrapEagerLoadEnabled() {
    return null;
  }

  @Override
  public Boolean getOverrideSystemProperties() {
    return null;
  }

  @Override
  public String getCacheDir() {
    return "";
  }

  @Override
  public String getCluster() {
    return "";
  }

  @Override
  public String getConfigService() {
    return "";
  }

  @Override
  public Boolean getClientMonitorEnabled() {
    return null;
  }

  @Override
  public Boolean getClientMonitorJmxEnabled() {
    return null;
  }

  @Override
  public String getClientMonitorExternalForm() {
    return "";
  }

  @Override
  public long getClientMonitorExternalExportPeriod() {
    return 0;
  }

  @Override
  public String getMeta() {
    return "";
  }

  @Override
  public String getMetaLatestFreshTime() {
    return "";
  }

  @Override
  public Boolean getPropertyNamesCacheEnable() {
    return null;
  }

  @Override
  public Boolean getPropertyOrderEnable() {
    return null;
  }

  @Override
  public String getVersion() {
    return "";
  }

  @Override
  public String getEnv() {
    return "";
  }

  @Override
  public String getAppId() {
    return "";
  }
}
