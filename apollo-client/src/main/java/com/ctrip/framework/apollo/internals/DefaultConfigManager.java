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

import static com.ctrip.framework.apollo.metrics.collector.ConfigCollector.CONFIG_MANAGER;
import static com.ctrip.framework.apollo.metrics.collector.ConfigCollector.NAMESPACE_USED_TIMES;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.metrics.Metrics;
import com.ctrip.framework.apollo.metrics.MetricsConstant;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.google.common.collect.Maps;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManager implements ConfigManager {

  protected Map<String, Config> m_configs = Maps.newConcurrentMap();
  protected Map<String, Object> m_configLocks = Maps.newConcurrentMap();
  protected Map<String, ConfigFile> m_configFiles = Maps.newConcurrentMap();
  protected Map<String, Object> m_configFileLocks = Maps.newConcurrentMap();
  private ConfigFactoryManager m_factoryManager;

  public DefaultConfigManager() {
    m_factoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);

  }

  @Override
  public Config getConfig(String namespace) {
    Config config = m_configs.get(namespace);
    if (config == null) {
      Object lock = m_configLocks.computeIfAbsent(namespace, key -> new Object());
      synchronized (lock) {
        config = m_configs.get(namespace);

        if (config == null) {
          ConfigFactory factory = m_factoryManager.getFactory(namespace);

          config = factory.create(namespace);
          m_configs.put(namespace, config);
        }
      }
    }

    Metrics.push(MetricsEvent.builder().withName(NAMESPACE_USED_TIMES)
        .putAttachment(MetricsConstant.NAMESPACE, namespace).withTag(CONFIG_MANAGER).build());

    return config;
  }

  @Override
  public ConfigFile getConfigFile(String namespace,
      ConfigFileFormat configFileFormat) {
    String namespaceFileName = String.format("%s.%s", namespace, configFileFormat.getValue());
    ConfigFile configFile = m_configFiles.get(namespaceFileName);

    if (configFile == null) {
      Object lock = m_configFileLocks.computeIfAbsent(namespaceFileName, key -> new Object());
      synchronized (lock) {
        configFile = m_configFiles.get(namespaceFileName);

        if (configFile == null) {
          ConfigFactory factory = m_factoryManager.getFactory(namespaceFileName);

          configFile = factory.createConfigFile(namespaceFileName, configFileFormat);
          m_configFiles.put(namespaceFileName, configFile);
        }
      }
    }

    return configFile;
  }
}
