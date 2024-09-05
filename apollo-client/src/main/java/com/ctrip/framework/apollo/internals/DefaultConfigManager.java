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

import java.util.Map;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManager implements ConfigManager {
  private ConfigFactoryManager m_factoryManager;

  private ConfigUtil m_configUtil;

  private Table<String,String,Config> m_configs = Tables.synchronizedTable(HashBasedTable.<String,String,Config>create());

  private Map<String, Object> m_configLocks = Maps.newConcurrentMap();

  private Table<String,String, ConfigFile> m_configFiles = HashBasedTable.create();

  private Map<String, Object> m_configFileLocks = Maps.newConcurrentMap();


  public DefaultConfigManager() {
    m_factoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public Config getConfig(String namespace) {
    return getConfig(m_configUtil.getAppId(), namespace);
  }

  @Override
  public Config getConfig(String appId, String namespace) {
    Config config = m_configs.get(appId, namespace);

    if (config == null) {
      Object lock = m_configLocks.computeIfAbsent(String.format("%s.%s", appId, namespace), key -> new Object());
      synchronized (lock) {
        config = m_configs.get(appId, namespace);

        if (config == null) {
          ConfigFactory factory = m_factoryManager.getFactory(appId, namespace);

          config = factory.create(appId, namespace);
          m_configs.put(appId, namespace, config);
        }
      }
    }
    return config;
  }

  @Override
  public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return getConfigFile(m_configUtil.getAppId(), namespace, configFileFormat);
  }

  @Override
  public ConfigFile getConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
    String namespaceFileName = String.format("%s.%s", namespace, configFileFormat.getValue());
    String lockNamespaceFileName = String.format("%s+%s.%s", appId, namespace, configFileFormat.getValue());
    ConfigFile configFile = m_configFiles.get(appId, namespaceFileName);

    if (configFile == null) {
      Object lock = m_configFileLocks.computeIfAbsent(lockNamespaceFileName, key -> new Object());
      synchronized (lock) {
        configFile = m_configFiles.get(appId, namespaceFileName);

        if (configFile == null) {
          ConfigFactory factory = m_factoryManager.getFactory(appId, namespaceFileName);

          configFile = factory.createConfigFile(appId, namespaceFileName, configFileFormat);
          m_configFiles.put(appId, namespaceFileName, configFile);
        }
      }
    }

    return configFile;
  }
}
