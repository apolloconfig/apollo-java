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

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.internals.JsonConfigFile;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.PropertiesCompatibleFileConfigRepository;
import com.ctrip.framework.apollo.internals.PropertiesConfigFile;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.internals.TxtConfigFile;
import com.ctrip.framework.apollo.internals.XmlConfigFile;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.internals.YmlConfigFile;
import com.ctrip.framework.apollo.internals.K8sConfigMapConfigRepository;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link ConfigFactory}.
 * <p>
 * Supports namespaces of format:
 * <ul>
 *   <li>{@link ConfigFileFormat#Properties}</li>
 *   <li>{@link ConfigFileFormat#XML}</li>
 *   <li>{@link ConfigFileFormat#JSON}</li>
 *   <li>{@link ConfigFileFormat#YML}</li>
 *   <li>{@link ConfigFileFormat#YAML}</li>
 *   <li>{@link ConfigFileFormat#TXT}</li>
 * </ul>
 *
 * @author Jason Song(song_s@ctrip.com)
 * @author Diego Krupitza(info@diegokrupitza.com)
 */
public class DefaultConfigFactory implements ConfigFactory {

  private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactory.class);
  private final ConfigUtil m_configUtil;

  public DefaultConfigFactory() {
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public Config create(String namespace) {
    return this.create(m_configUtil.getAppId(), namespace);
  }

  @Override
  public Config create(String appId, String namespace) {
    ConfigFileFormat format = determineFileFormat(namespace);

    ConfigRepository configRepository = null;

    // although ConfigFileFormat.Properties are compatible with themselves we
    // should not create a PropertiesCompatibleFileConfigRepository for them
    // calling the method `createLocalConfigRepository(...)` is more suitable
    // for ConfigFileFormat.Properties
    if (ConfigFileFormat.isPropertiesCompatible(format) &&
            format != ConfigFileFormat.Properties) {
      configRepository = createPropertiesCompatibleFileConfigRepository(appId, namespace, format);
    } else {
      configRepository = createConfigRepository(appId, namespace);
    }

    logger.debug("Created a configuration repository of type [{}] for namespace [{}]",
            configRepository.getClass().getName(), namespace);

    return this.createRepositoryConfig(appId, namespace, configRepository);
  }

  @Override
  public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return this.createConfigFile(m_configUtil.getAppId(), namespace, configFileFormat);
  }

  protected Config createRepositoryConfig(String appId, String namespace, ConfigRepository configRepository) {
    return new DefaultConfig(appId, namespace, configRepository);
  }

  @Override
  public ConfigFile createConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
    ConfigRepository configRepository = createConfigRepository(appId, namespace);
    switch (configFileFormat) {
      case Properties:
        return new PropertiesConfigFile(appId, namespace, configRepository);
      case XML:
        return new XmlConfigFile(appId, namespace, configRepository);
      case JSON:
        return new JsonConfigFile(appId, namespace, configRepository);
      case YAML:
        return new YamlConfigFile(appId, namespace, configRepository);
      case YML:
        return new YmlConfigFile(appId, namespace, configRepository);
      case TXT:
        return new TxtConfigFile(appId, namespace, configRepository);
    }

    return null;
  }

  ConfigRepository createConfigRepository(String appId, String namespace) {
    if (m_configUtil.isPropertyKubernetesCacheEnabled()) {
      return createConfigMapConfigRepository(appId, namespace);
    } else if (m_configUtil.isPropertyFileCacheEnabled()) {
      return createLocalConfigRepository(appId, namespace);
    }
    return createRemoteConfigRepository(appId, namespace);
  }

  /**
   * Creates a local repository for a given namespace
   *
   * @param appId the appId of the repository
   * @param namespace the namespace of the repository
   * @return the newly created repository for the given namespace
   */
  LocalFileConfigRepository createLocalConfigRepository(String appId, String namespace) {
    if (m_configUtil.isInLocalMode()) {
      logger.warn(
          "==== Apollo is in local mode! Won't pull configs from remote server for namespace {} ! ====",
          namespace);
      return new LocalFileConfigRepository(appId, namespace);
    }
    return new LocalFileConfigRepository(appId, namespace, createRemoteConfigRepository(appId, namespace));
  }

  /**
   * Creates a Kubernetes config map repository for a given namespace
   * @param namespace the namespace of the repository
   * @return the newly created repository for the given namespace
   */
  private ConfigRepository createConfigMapConfigRepository(String appId, String namespace) {
    return new K8sConfigMapConfigRepository(appId, namespace, createLocalConfigRepository(appId, namespace));
  }
  RemoteConfigRepository createRemoteConfigRepository(String appId, String namespace) {
    return new RemoteConfigRepository(appId, namespace);
  }

  PropertiesCompatibleFileConfigRepository createPropertiesCompatibleFileConfigRepository(
      String appId, String namespace, ConfigFileFormat format) {
    String actualNamespaceName = trimNamespaceFormat(namespace, format);
    PropertiesCompatibleConfigFile configFile = (PropertiesCompatibleConfigFile) ConfigService
        .getConfigFile(actualNamespaceName, format);

    return new PropertiesCompatibleFileConfigRepository(configFile);
  }

  // for namespaces whose format are not properties, the file extension must be present, e.g. application.yaml
  ConfigFileFormat determineFileFormat(String namespaceName) {
    String lowerCase = namespaceName.toLowerCase();
    for (ConfigFileFormat format : ConfigFileFormat.values()) {
      if (lowerCase.endsWith("." + format.getValue())) {
        return format;
      }
    }

    return ConfigFileFormat.Properties;
  }

  String trimNamespaceFormat(String namespaceName, ConfigFileFormat format) {
    String extension = "." + format.getValue();
    if (!namespaceName.toLowerCase().endsWith(extension)) {
      return namespaceName;
    }

    return namespaceName.substring(0, namespaceName.length() - extension.length());
  }

}
