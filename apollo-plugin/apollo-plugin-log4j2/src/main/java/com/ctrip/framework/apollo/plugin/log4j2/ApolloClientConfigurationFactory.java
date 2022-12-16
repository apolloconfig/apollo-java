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

package com.ctrip.framework.apollo.plugin.log4j2;

import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * @author nisiyong
 */
@Plugin(name = "ApolloClientConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class ApolloClientConfigurationFactory extends ConfigurationFactory {

  private final boolean isActive;

  public ApolloClientConfigurationFactory() {
    String enabled = System.getProperty("apollo.log4j2.enabled");
    if (enabled == null) {
      enabled = System.getenv("APOLLO_LOG4J2_ENABLED");
    }
    isActive = Boolean.parseBoolean(enabled);
  }

  @Override
  protected boolean isActive() {
    return this.isActive;
  }

  @Override
  protected String[] getSupportedTypes() {
    return new String[]{"*"};
  }

  @Override
  public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation) {
    return getConfiguration(loggerContext, null);
  }

  @Override
  public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource configurationSource) {
    if (!isActive) {
      LOGGER.warn("Apollo log4j2 plugin is not enabled, please check your configuration");
      return null;
    }

    ConfigFile configFile = ConfigService.getConfigFile("log4j2", ConfigFileFormat.XML);

    if (configFile == null || configFile.getContent() == null || configFile.getContent().isEmpty()) {
      LOGGER.warn("Apollo log4j2 plugin is enabled, but no log4j2.xml namespace or content found in Apollo");
      return null;
    }

    byte[] bytes = configFile.getContent().getBytes(StandardCharsets.UTF_8);
    try {
      configurationSource = new ConfigurationSource(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new ConfigurationException("Unable to initialize ConfigurationSource from Apollo", e);
    }

    // TODO add ConfigFileChangeListener, dynamic load log4j2.xml in runtime
    LOGGER.info("Apollo log4j2 plugin is enabled, loading log4j2.xml from Apollo, content:\n{}", configFile.getContent());
    return new XmlConfiguration(loggerContext, configurationSource);
  }
}
