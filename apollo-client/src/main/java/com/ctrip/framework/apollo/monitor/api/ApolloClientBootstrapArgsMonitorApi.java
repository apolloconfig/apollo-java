/*
 * Copyright 2022  Authors
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

/**
 * @author Rawven
 */
public interface ApolloClientBootstrapArgsMonitorApi {

  /**
   * get bootstrap args map
   */
  Map<String, String> getBootstrapArgs();

  /**
   * get startup params by key
   */
  String getStartupParams(String key);

  /**
   *  config service url
   */
  String getConfigServiceUrl();

  /**
   *  access key secret
   */
  String getAccessKeySecret();

  /**
   * auto update injected spring properties
   */
  Boolean getAutoUpdateInjectedSpringProperties();

  Boolean getBootstrapEnabled();

  String getBootstrapNamespaces();

  Boolean getBootstrapEagerLoadEnabled();

  Boolean getOverrideSystemProperties();

  String getCacheDir();

  String getCluster();

  String getConfigService();

  Boolean getClientMonitorEnabled();

  Boolean getClientMonitorJmxEnabled();

  String getClientMonitorExternalForm();

  long getClientMonitorExternalExportPeriod();

  int getClientMonitorExceptionSaveSize();

  String getApolloMeta();

  String getMetaLatestFreshTime();

  Boolean getPropertyNamesCacheEnable();

  Boolean getPropertyOrderEnable();

  String getVersion();

  String getEnv();

  String getAppId();
}
