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
package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactoryManager implements ConfigFactoryManager {
  private ConfigRegistry m_registry;

  private Table<String, String, ConfigFactory> m_factories = Tables.synchronizedTable(HashBasedTable.create());

  private ConfigUtil m_configUtil;

  public DefaultConfigFactoryManager() {
    m_registry = ApolloInjector.getInstance(ConfigRegistry.class);
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public ConfigFactory getFactory(String namespace) {
    return getFactory(m_configUtil.getAppId(), namespace);
  }

  @Override
  public ConfigFactory getFactory(String appId, String namespace) {
    // step 1: check hacked factory
    ConfigFactory factory = m_registry.getFactory(appId, namespace);

    if (factory != null) {
      return factory;
    }

    // step 2: check cache
    factory = m_factories.get(appId, namespace);

    if (factory != null) {
      return factory;
    }

    // step 3: check declared config factory
    factory = ApolloInjector.getInstance(ConfigFactory.class, namespace);

    if (factory != null) {
      return factory;
    }

    // step 4: check default config factory
    factory = ApolloInjector.getInstance(ConfigFactory.class);

    m_factories.put(appId, namespace, factory);

    // factory should not be null
    return factory;
  }
}
