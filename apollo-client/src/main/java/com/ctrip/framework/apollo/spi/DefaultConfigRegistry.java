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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigRegistry implements ConfigRegistry {
  private static final Logger s_logger = LoggerFactory.getLogger(DefaultConfigRegistry.class);

  private ConfigUtil m_configUtil;

  private Table<String, String, ConfigFactory> m_instances = Tables.synchronizedTable(
      HashBasedTable.create());

  public DefaultConfigRegistry() {
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public void register(String namespace, ConfigFactory factory) {
    register(m_configUtil.getAppId(), namespace, factory);
  }

  @Override
  public void register(String appId, String namespace, ConfigFactory factory) {
    if (m_instances.contains(appId, namespace)) {
      s_logger.warn("ConfigFactory({}-{}) is overridden by {}!", appId, namespace, factory.getClass());
    }

    m_instances.put(appId, namespace, factory);
  }

  @Override
  public ConfigFactory getFactory(String namespace) {
    return getFactory(m_configUtil.getAppId(), namespace);
  }

  @Override
  public ConfigFactory getFactory(String appId, String namespace) {
    return m_instances.get(appId, namespace);
  }
}
