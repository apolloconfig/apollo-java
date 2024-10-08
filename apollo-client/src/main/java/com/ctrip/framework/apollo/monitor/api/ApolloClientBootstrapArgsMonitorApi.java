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

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.*;
import static com.ctrip.framework.apollo.core.ConfigConsts.APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.*;

import java.util.Collections;
import java.util.Map;

/**
 * @author Rawven
 */
public interface ApolloClientBootstrapArgsMonitorApi {

  /**
   * get startup params by key
   */
  default Object getStartupArg(String key) {
    return getBootstrapArgs().get(key);
  }

  /**
   * get bootstrap args map
   */
  default Map<String, Object> getBootstrapArgs() {
    return Collections.emptyMap();
  }

  default String getConfigServiceUrl() {
    return (String) getBootstrapArgs().getOrDefault(CONFIG_SERVICE_URL, "");
  }

  default String getAccessKeySecret() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_ACCESS_KEY_SECRET, "");
  }

  default Boolean getAutoUpdateInjectedSpringProperties() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES,
        false);
  }

  default Boolean isBootstrapEnabled() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_BOOTSTRAP_ENABLED, false);
  }

  default String getBootstrapNamespaces() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_BOOTSTRAP_NAMESPACES, "");
  }

  default Boolean isBootstrapEagerLoadEnabled() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, false);
  }

  default Boolean isOverrideSystemProperties() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_OVERRIDE_SYSTEM_PROPERTIES, false);
  }

  default String getCacheDir() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_CACHE_DIR, "");
  }

  default String getCluster() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_CLUSTER, "");
  }

  default String getConfigService() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_CONFIG_SERVICE, "");
  }

  default String getClientMonitorExternalForm() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE, "");
  }

  default Boolean isClientMonitorEnabled() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_CLIENT_MONITOR_ENABLED, false);
  }

  default Boolean isClientMonitorJmxEnabled() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_CLIENT_MONITOR_JMX_ENABLED, false);
  }

  default long getClientMonitorExternalExportPeriod() {
    return (Long) getBootstrapArgs().getOrDefault(APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD, 0L);
  }

  default int getClientMonitorExceptionSaveSize() {
    return (Integer) getBootstrapArgs().getOrDefault(APOLLO_CLIENT_MONITOR_EXCEPTION_QUEUE_SIZE, 0);
  }

  default String getApolloMeta() {
    return (String) getBootstrapArgs().getOrDefault(APOLLO_META, "");
  }

  default String getMetaLatestFreshTime() {
    return (String) getBootstrapArgs().getOrDefault(META_FRESH, "");
  }

  default Boolean isPropertyNamesCacheEnable() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_PROPERTY_NAMES_CACHE_ENABLE, false);
  }

  default Boolean isPropertyOrderEnable() {
    return (Boolean) getBootstrapArgs().getOrDefault(APOLLO_PROPERTY_ORDER_ENABLE, false);
  }

  default String getVersion() {
    return (String) getBootstrapArgs().getOrDefault(VERSION, "");
  }

  default String getEnv() {
    return (String) getBootstrapArgs().getOrDefault(ENV, "");
  }

  default String getAppId() {
    return (String) getBootstrapArgs().getOrDefault(APP_ID, "");
  }
}