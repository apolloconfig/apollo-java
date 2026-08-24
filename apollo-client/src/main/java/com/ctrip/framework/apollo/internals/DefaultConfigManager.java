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
package com.ctrip.framework.apollo.internals;

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.APOLLO_CLIENT_NAMESPACE_USAGE;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.HashBasedTable;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManager implements ConfigManager {
  private ConfigFactoryManager factoryManager;

  private ConfigUtil configUtil;

  private Table<String, String, Config> configs = Tables.synchronizedTable(HashBasedTable.create());

  private Map<String, Object> configLocks = Maps.newConcurrentMap();

  private Table<String, String, ConfigFile> configFiles = Tables.synchronizedTable(HashBasedTable.create());

  private Map<String, Object> configFileLocks = Maps.newConcurrentMap();


  public DefaultConfigManager() {
    factoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public Config getConfig(String namespace) {
    return getConfig(configUtil.getAppId(), namespace);
  }
    
  @Override
  public Config getConfig(String appId, String namespace) {
    Config config = configs.get(appId, namespace);

    if (config == null) {
      Object lock = configLocks.computeIfAbsent(String.format("%s.%s", appId, namespace), key -> new Object());
      synchronized (lock) {
        config = configs.get(appId, namespace);

        if (config == null) {
          ConfigFactory factory = factoryManager.getFactory(appId, namespace);

          config = factory.create(appId, namespace);
          configs.put(appId, namespace, config);
        }
      }
    }
    if (!ConfigSourceType.NONE.equals(config.getSourceType())) {
      Tracer.logMetricsForCount(APOLLO_CLIENT_NAMESPACE_USAGE + ":" + namespace);
    }

    return config;
  }

  @Override
  public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return getConfigFile(configUtil.getAppId(), namespace, configFileFormat);
  }

  @Override
  public ConfigFile getConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
    String namespaceFileName = String.format("%s.%s", namespace, configFileFormat.getValue());
    String lockNamespaceFileName = String.format("%s+%s.%s", appId, namespace, configFileFormat.getValue());
    ConfigFile configFile = configFiles.get(appId, namespaceFileName);

    if (configFile == null) {
      Object lock = configFileLocks.computeIfAbsent(lockNamespaceFileName, key -> new Object());
      synchronized (lock) {
        configFile = configFiles.get(appId, namespaceFileName);

        if (configFile == null) {
          ConfigFactory factory = factoryManager.getFactory(appId, namespaceFileName);

          configFile = factory.createConfigFile(appId, namespaceFileName, configFileFormat);
          configFiles.put(appId, namespaceFileName, configFile);
        }
      }
    }

    return configFile;
  }
}
