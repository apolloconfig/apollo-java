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

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class SimpleConfig extends AbstractConfig implements RepositoryChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(SimpleConfig.class);
  private final String appId;
  private final String namespace;
  private final ConfigRepository configRepository;
  private volatile Properties configProperties;
  private volatile ConfigSourceType sourceType = ConfigSourceType.NONE;

  /**
   * Constructor.
   *
   * @param namespace        the namespace for this config instance
   * @param configRepository the config repository for this config instance
   */
  public SimpleConfig(String namespace, ConfigRepository configRepository) {
    this(null, namespace, configRepository);
  }

  /**
   * Constructor.
   *
   * @param appId        the appId for this config instance
   * @param namespace        the namespace for this config instance
   * @param configRepository the config repository for this config instance
   */
  public SimpleConfig(String appId, String namespace, ConfigRepository configRepository) {
    if (appId == null) {
      appId = ApolloInjector.getInstance(ConfigUtil.class).getAppId();
    }
    this.appId = appId;
    this.namespace = namespace;
    this.configRepository = configRepository;
    this.initialize();
  }

  private void initialize() {
    try {
      updateConfig(configRepository.getConfig(), configRepository.getSourceType());
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger.warn("Init Apollo Simple Config failed - namespace: {}, reason: {}", namespace,
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      //register the change listener no matter config repository is working or not
      //so that whenever config repository is recovered, config could get changed
      configRepository.addChangeListener(this);
    }
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    if (configProperties == null) {
      logger.warn("Could not load config from Apollo, always return default value!");
      return defaultValue;
    }
    return this.configProperties.getProperty(key, defaultValue);
  }

  @Override
  public Set<String> getPropertyNames() {
    if (configProperties == null) {
      return Collections.emptySet();
    }

    return configProperties.stringPropertyNames();
  }

  @Override
  public ConfigSourceType getSourceType() {
    return sourceType;
  }

  @Override
  public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
    this.onRepositoryChange(appId, namespace, newProperties);
  }

  @Override
  public synchronized void onRepositoryChange(String appId, String namespace, Properties newProperties) {
    if (newProperties.equals(configProperties)) {
      return;
    }
    Properties newConfigProperties = propertiesFactory.getPropertiesInstance();
    newConfigProperties.putAll(newProperties);

    List<ConfigChange> changes = calcPropertyChanges(appId, namespace, configProperties, newConfigProperties);
    Map<String, ConfigChange> changeMap = Maps.uniqueIndex(changes,
        new Function<ConfigChange, String>() {
          @Override
          public String apply(ConfigChange input) {
            return input.getPropertyName();
          }
        });

    updateConfig(newConfigProperties, configRepository.getSourceType());
    clearConfigCache();

    this.fireConfigChange(appId, this.namespace, changeMap);

    Tracer.logEvent(APOLLO_CLIENT_CONFIGCHANGES, this.namespace);
  }

  private void updateConfig(Properties newConfigProperties, ConfigSourceType sourceType) {
    configProperties = newConfigProperties;
    this.sourceType = sourceType;
  }
}
