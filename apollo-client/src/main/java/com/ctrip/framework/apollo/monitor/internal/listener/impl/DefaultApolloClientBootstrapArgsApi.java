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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.*;
import static com.ctrip.framework.apollo.core.ConfigConsts.APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES;
import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.*;

import com.ctrip.framework.apollo.Apollo;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.monitor.api.ApolloClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxBootstrapArgsMBean;
import com.ctrip.framework.apollo.monitor.internal.listener.AbstractApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.event.ApolloClientMonitorEvent;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.Maps;
import java.util.Map;
import org.slf4j.Logger;

/**
 * @author Rawven
 */
public class DefaultApolloClientBootstrapArgsApi extends
    AbstractApolloClientMonitorEventListener implements
    ApolloClientBootstrapArgsMonitorApi, ApolloClientJmxBootstrapArgsMBean {

  private static final Logger logger = DeferredLoggerFactory.getLogger(
      DefaultApolloClientBootstrapArgsApi.class);
  private final Map<String, Object> bootstrapArgs = Maps.newHashMap();
  private final Map<String, String> bootstrapArgsString = Maps.newHashMap();

  public DefaultApolloClientBootstrapArgsApi(ConfigUtil configUtil) {
    super(TAG_BOOTSTRAP);
    bootstrapArgs.put(APOLLO_ACCESS_KEY_SECRET, configUtil.getAccessKeySecret());
    bootstrapArgs.put(APOLLO_AUTO_UPDATE_INJECTED_SPRING_PROPERTIES,
        configUtil.isAutoUpdateInjectedSpringPropertiesEnabled());
    bootstrapArgs.put(APOLLO_BOOTSTRAP_ENABLED,
        Boolean.parseBoolean(System.getProperty(APOLLO_BOOTSTRAP_ENABLED)));
    bootstrapArgs.put(APOLLO_BOOTSTRAP_NAMESPACES,
        System.getProperty(APOLLO_BOOTSTRAP_NAMESPACES));
    bootstrapArgs.put(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED,
        Boolean.parseBoolean(System.getProperty(APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED)));
    bootstrapArgs.put(APOLLO_OVERRIDE_SYSTEM_PROPERTIES, configUtil.isOverrideSystemProperties());
    bootstrapArgs.put(APOLLO_CACHE_DIR, configUtil.getDefaultLocalCacheDir());
    bootstrapArgs.put(APOLLO_CLUSTER, configUtil.getCluster());
    bootstrapArgs.put(APOLLO_CONFIG_SERVICE,
        System.getProperty(APOLLO_CONFIG_SERVICE));
    bootstrapArgs.put(APOLLO_CLIENT_MONITOR_EXTERNAL_TYPE, configUtil.getMonitorExternalType());
    bootstrapArgs.put(APOLLO_CLIENT_MONITOR_ENABLED, configUtil.isClientMonitorEnabled());
    bootstrapArgs.put(APOLLO_CLIENT_MONITOR_EXTERNAL_EXPORT_PERIOD,
        configUtil.getMonitorExternalExportPeriod());
    bootstrapArgs.put(APOLLO_META, configUtil.getMetaServerDomainName());
    bootstrapArgs.put(APOLLO_PROPERTY_NAMES_CACHE_ENABLE, configUtil.isPropertyNamesCacheEnabled());
    bootstrapArgs.put(APOLLO_PROPERTY_ORDER_ENABLE, configUtil.isPropertiesOrderEnabled());
    bootstrapArgs.put(APOLLO_CLIENT_MONITOR_JMX_ENABLED, configUtil.isClientMonitorJmxEnabled());
    bootstrapArgs.put(APOLLO_CLIENT_MONITOR_EXCEPTION_QUEUE_SIZE,
        configUtil.getMonitorExceptionQueueSize());
    bootstrapArgs.put(APP_ID, configUtil.getAppId());
    bootstrapArgs.put(ENV, configUtil.getApolloEnv());
    bootstrapArgs.put(VERSION, Apollo.VERSION);
    bootstrapArgs.forEach((key, value) -> {
      if (value != null) {
        bootstrapArgsString.put(key, value.toString());
      }
    });

  }

  @Override
  public void collect0(ApolloClientMonitorEvent event) {
    String argName = event.getName();
    if (bootstrapArgs.containsKey(argName)) {
      bootstrapArgs.put(argName, event.getAttachmentValue(argName));
    } else {
      logger.warn("Unhandled event name: {}", argName);
    }
  }

  @Override
  public boolean isMetricsSampleUpdated() {
    return false;
  }

  @Override
  public Map<String, Object> getBootstrapArgs() {
    return bootstrapArgs;
  }

  @Override
  public Map<String, String> getBootstrapArgsString() {
    return bootstrapArgsString;
  }
}
