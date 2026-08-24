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
package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.internals.ConfigMonitorInitializer;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * Entry point for client config use
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigService {
  private static final ConfigService instance = new ConfigService();
  private volatile ConfigMonitor configMonitor;
  private volatile ConfigManager configManager;
  private volatile ConfigRegistry configRegistry;
  
  private ConfigMonitor getMonitor() {
      getManager();
      if (configMonitor == null) {
            synchronized (this) {
                if (configMonitor == null) {
                  configMonitor = ApolloInjector.getInstance(ConfigMonitor.class);
                }
            }
      }
      return configMonitor;
  }
  
  private ConfigManager getManager() {
    if (configManager == null) {
      synchronized (this) {
        if (configManager == null) {
          configManager = ApolloInjector.getInstance(ConfigManager.class);
          ConfigMonitorInitializer.initialize();
        }
      }
    }
    return configManager;
  }

  private ConfigRegistry getRegistry() {
    if (configRegistry == null) {
      synchronized (this) {
        if (configRegistry == null) {
          configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
        }
      }
    }

    return configRegistry;
  }

  /**
   * Get Application's config instance.
   *
   * @return config instance
   */
  public static Config getAppConfig() {
    return getConfig(ConfigConsts.NAMESPACE_APPLICATION);
  }

  /**
   * Get the config instance for the namespace.
   *
   * @param namespace the namespace of the config
   * @return config instance
   */
  public static Config getConfig(String namespace) {
    return instance.getManager().getConfig(namespace);
  }

  public static Config getConfig(String appId, String namespace) {
    return instance.getManager().getConfig(appId, namespace);
  }

  public static ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return instance.getManager().getConfigFile(namespace, configFileFormat);
  }

  /**
   * Get the config file instance for the appId and namespace.
   *
   * @param appId            the appId of the config
   * @param namespace        the namespace of the config without file extension, e.g. "application"
   * @param configFileFormat the config file format
   * @return config file instance
   */
  public static ConfigFile getConfigFile(String appId, String namespace,
      ConfigFileFormat configFileFormat) {
    return instance.getManager().getConfigFile(appId, namespace, configFileFormat);
  }

  public static ConfigMonitor getConfigMonitor(){
      return instance.getMonitor();
  }

  static void setConfig(Config config) {
    setConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
  }

  /**
   * Manually set the config for the namespace specified, use with caution.
   *
   * @param namespace the namespace
   * @param config    the config instance
   */
  static void setConfig(String namespace, final Config config) {
    instance.getRegistry().register(namespace, new ConfigFactory() {

      private final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);

      @Override
      public Config create(String namespace) {
        return config;
      }

      @Override
      public Config create(String appId, String namespace) {
        if(!StringUtils.equals(appId, configUtil.getAppId())){
          throw new IllegalArgumentException("Provided appId '" + appId + "' does not match the default appId '" + configUtil.getAppId() + "'");
        }
        return config;
      }

      @Override
      public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        return null;
      }

      @Override
      public ConfigFile createConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
        return null;
      }

    });
  }

  static void setConfigFactory(ConfigFactory factory) {
    setConfigFactory(ConfigConsts.NAMESPACE_APPLICATION, factory);
  }

  /**
   * Manually set the config factory for the namespace specified, use with caution.
   *
   * @param namespace the namespace
   * @param factory   the factory instance
   */
  static void setConfigFactory(String namespace, ConfigFactory factory) {
    instance.getRegistry().register(namespace, factory);
  }

  // for test only
  static void reset() {
    synchronized (instance) {
      instance.configManager = null;
      instance.configRegistry = null;
    }
  }
}
