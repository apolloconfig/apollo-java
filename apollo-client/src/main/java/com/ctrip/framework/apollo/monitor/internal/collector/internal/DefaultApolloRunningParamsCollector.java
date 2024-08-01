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
package com.ctrip.framework.apollo.monitor.internal.collector.internal;

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CACHE_DIR;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_ENABLED;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLIENT_MONITOR_JMX_ENABLED;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLUSTER;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_META;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_OVERRIDE_SYSTEM_PROPERTIES;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_PROPERTY_ORDER_ENABLE;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APP_ID;
import static com.ctrip.framework.apollo.core.ConfigConsts.APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES;

import com.ctrip.framework.apollo.Apollo;
import com.ctrip.framework.apollo.monitor.api.ApolloRunningParamsMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.collector.AbstractMetricsCollector;
import com.ctrip.framework.apollo.monitor.internal.model.MetricsEvent;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.Maps;
import java.util.Map;

/**
 * @author Rawven
 */
public class DefaultApolloRunningParamsCollector extends AbstractMetricsCollector implements
    ApolloRunningParamsMonitorApi {

  public static final String ENV = "env";
  public static final String VERSION = "version";
  public static final String RUNNING_PARAMS = "RunningParams";
  public static final String META_FRESH = "metaFreshTime";
  public static final String CONFIG_SERVICE_URL = "configServiceUrl";
  private final Map<String, Object> map = Maps.newHashMap();

  public DefaultApolloRunningParamsCollector(ConfigUtil configUtil) {
    super(RUNNING_PARAMS, RUNNING_PARAMS);
    map.put(APOLLO_ACCESS_KEY_SECRET, configUtil.getAccessKeySecret());
    map.put(APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES,
        configUtil.isAutoUpdateInjectedSpringPropertiesEnabled());
    map.put(APOLLO_BOOTSTRAP_ENABLED,
        Boolean.parseBoolean(System.getProperty(APOLLO_BOOTSTRAP_ENABLED)));
    map.put(APOLLO_BOOTSTRAP_NAMESPACES,
        System.getProperty(APOLLO_BOOTSTRAP_NAMESPACES));
    map.put(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED,
        Boolean.parseBoolean(System.getProperty(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED)));
    map.put(APOLLO_OVERRIDE_SYSTEM_PROPERTIES, configUtil.isOverrideSystemProperties());
    map.put(APOLLO_CACHE_DIR, configUtil.getDefaultLocalCacheDir());
    map.put(APOLLO_CLUSTER, configUtil.getCluster());
    map.put(APOLLO_CONFIG_SERVICE,
        System.getProperty(APOLLO_CONFIG_SERVICE));
    map.put(APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE, configUtil.getMonitorExternalType());
    map.put(APOLLO_CLIENT_MONITOR_ENABLED, configUtil.isClientMonitorEnabled());
    map.put(APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD,
        configUtil.getMonitorExternalExportPeriod());
    map.put(APOLLO_META, configUtil.getMetaServerDomainName());
    map.put(APOLLO_PROPERTY_NAMES_CACHE_ENABLE, configUtil.isPropertyNamesCacheEnabled());
    map.put(APOLLO_PROPERTY_ORDER_ENABLE, configUtil.isPropertiesOrderEnabled());
    map.put(APOLLO_CLIENT_MONITOR_JMX_ENABLED, configUtil.isClientMonitorJmxEnabled());
    map.put(APP_ID, configUtil.getAppId());
    map.put(ENV, configUtil.getApolloEnv());
    map.put(VERSION, Apollo.VERSION);
  }

  @Override
  public String name() {
    return RUNNING_PARAMS;
  }

  @Override
  public void collect0(MetricsEvent event) {
    switch (event.getName()) {
      case VERSION:
        map.put(VERSION, event.getAttachmentValue(VERSION));
        break;
      case META_FRESH:
        map.put(META_FRESH, event.getAttachmentValue(META_FRESH));
        break;
      case CONFIG_SERVICE_URL:
        map.put(CONFIG_SERVICE_URL, event.getAttachmentValue(CONFIG_SERVICE_URL));
        break;
      default:
        break;
    }
  }

  @Override
  public boolean isSamplesUpdated() {
    return false;
  }

  @Override
  public void export0() {
  }

  @Override
  public String getStartupParams(String key) {
    return map.getOrDefault(key, "").toString();
  }

  @Override
  public String getConfigServiceUrl() {
    return map.get(CONFIG_SERVICE_URL).toString();
  }


  @Override
  public String getAccessKeySecret() {
    return map.getOrDefault(APOLLO_ACCESS_KEY_SECRET, "").toString();
  }

  @Override
  public Boolean getAutoUpdateInjectedSpringProperties() {
    return (Boolean) map.get(APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES);
  }

  @Override
  public Boolean getBootstrapEnabled() {
    return (Boolean) map.get(APOLLO_BOOTSTRAP_ENABLED);
  }

  @Override
  public String getBootstrapNamespaces() {
    return (String) map.get(APOLLO_BOOTSTRAP_NAMESPACES);
  }

  @Override
  public Boolean getBootstrapEagerLoadEnabled() {
    return (Boolean) map.get(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED);
  }

  @Override
  public Boolean getOverrideSystemProperties() {
    return (Boolean) map.get(APOLLO_OVERRIDE_SYSTEM_PROPERTIES);
  }

  @Override
  public String getCacheDir() {
    return map.get(APOLLO_CACHE_DIR).toString();
  }

  @Override
  public String getCluster() {
    return map.get(
        APOLLO_CLUSTER).toString();
  }

  @Override
  public String getConfigService() {
    return map.get(APOLLO_CONFIG_SERVICE).toString();
  }

  @Override
  public String getClientMonitorExternalForm() {
    return map.get(APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE).toString();
  }

  @Override
  public Boolean getClientMonitorEnabled() {
    return (Boolean) map.get(APOLLO_CLIENT_MONITOR_ENABLED);
  }

  @Override
  public Boolean getClientMonitorJmxEnabled() {
    return (Boolean) map.get(APOLLO_CLIENT_MONITOR_JMX_ENABLED);
  }

  @Override
  public long getClientMonitorExternalExportPeriod() {
    return (Long) map.get(APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD);
  }

  @Override
  public String getMeta() {
    return map.get(APOLLO_META).toString();
  }

  @Override
  public Boolean getPropertyNamesCacheEnable() {
    return (Boolean) map.get(APOLLO_PROPERTY_NAMES_CACHE_ENABLE);
  }

  @Override
  public Boolean getPropertyOrderEnable() {
    return (Boolean) map.get(APOLLO_PROPERTY_ORDER_ENABLE);
  }

  @Override
  public String getMetaLatestFreshTime() {
    return map.get(META_FRESH).toString();
  }

  @Override
  public String getVersion() {
    return map.get(VERSION).toString();
  }

  @Override
  public String getEnv() {
    return map.get(ENV).toString();
  }

  @Override
  public String getAppId() {
    return map.get(APP_ID).toString();
  }

}
