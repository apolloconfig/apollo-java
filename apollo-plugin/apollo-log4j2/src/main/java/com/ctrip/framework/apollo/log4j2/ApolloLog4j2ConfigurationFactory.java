package com.ctrip.framework.apollo.log4j2;

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
import org.apache.logging.log4j.util.Strings;

/**
 * @author nisiyong
 */
@Plugin(name = "ApolloLog4j2ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class ApolloLog4j2ConfigurationFactory extends ConfigurationFactory {

  private final boolean isActive;

  public ApolloLog4j2ConfigurationFactory() {
    String enabled = System.getProperty("apollo.log4j2.enabled");
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
      return null;
    }

    ConfigFile configFile = ConfigService.getConfigFile("log4j2", ConfigFileFormat.XML);

    if (configFile == null || Strings.isBlank(configFile.getContent())) {
      return null;
    }

    byte[] bytes = configFile.getContent().getBytes(StandardCharsets.UTF_8);
    try {
      configurationSource = new ConfigurationSource(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new ConfigurationException("Unable to initialize ConfigurationSource from Apollo", e);
    }

    // TODO add ConfigFileChangeListener, dynamic load log4j2.xml in runtime
    LOGGER.debug("Initializing configuration ApolloLog4j2Configuration[namespace=log4j2.xml]\n{}", configFile.getContent());
    return new XmlConfiguration(loggerContext, configurationSource);
  }
}
