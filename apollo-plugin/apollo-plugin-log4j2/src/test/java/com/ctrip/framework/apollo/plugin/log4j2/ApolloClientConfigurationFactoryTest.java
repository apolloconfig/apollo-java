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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.ctrip.framework.apollo.internals.ConfigManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import com.google.common.collect.Table;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApolloClientConfigurationFactoryTest {

  private static final String ORIGINAL_APP_ID = System.getProperty("app.id");
  private static final String ORIGINAL_ENV = System.getProperty("env");
  private static final String ORIGINAL_ENABLED = System.getProperty("apollo.log4j2.enabled");
  private static final String LOG4J2_NAMESPACE = "log4j2.xml";

  static {
    System.setProperty("app.id", "someAppId");
    System.setProperty("env", "local");
  }

  @Before
  public void setUp() throws Exception {
    resetConfigService();
    clearApolloCaches();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    restoreOrClear("app.id", ORIGINAL_APP_ID);
    restoreOrClear("env", ORIGINAL_ENV);
    restoreOrClear("apollo.log4j2.enabled", ORIGINAL_ENABLED);
    resetConfigService();
    clearApolloCaches();
  }

  @Test
  public void test1ShouldReturnNullWhenPluginIsDisabled() {
    System.setProperty("apollo.log4j2.enabled", "false");

    ApolloClientConfigurationFactory factory = new ApolloClientConfigurationFactory();
    Configuration configuration = factory.getConfiguration(new LoggerContext("disabled"), null);

    assertNull(configuration);
  }

  @Test
  public void test2ShouldReturnNullWhenNoLog4j2NamespaceContent() throws Exception {
    System.setProperty("apollo.log4j2.enabled", "true");
    registerConfigFile(null);
    ConfigFile configFile = ConfigService.getConfigFile("log4j2", ConfigFileFormat.XML);
    assertNotNull(configFile);
    assertNull(configFile.getContent());

    ApolloClientConfigurationFactory factory = new ApolloClientConfigurationFactory();
    Configuration configuration = factory.getConfiguration(new LoggerContext("empty"), null);

    assertNull(configuration);
  }

  @Test
  public void test3ShouldBuildXmlConfigurationWhenContentExists() throws Exception {
    System.setProperty("apollo.log4j2.enabled", "true");
    registerConfigFile(
        "<Configuration status=\"WARN\"><Appenders/><Loggers><Root level=\"INFO\"/></Loggers></Configuration>");

    ApolloClientConfigurationFactory factory = new ApolloClientConfigurationFactory();
    Configuration configuration = factory.getConfiguration(new LoggerContext("apollo"), null);

    assertNotNull(configuration);
  }

  private static void registerConfigFile(String content) throws Exception {
    ConfigFactory factory = new StaticConfigFactory(content);
    Method setFactoryMethod =
        ConfigService.class.getDeclaredMethod("setConfigFactory", String.class, ConfigFactory.class);
    setFactoryMethod.setAccessible(true);
    setFactoryMethod.invoke(null, LOG4J2_NAMESPACE, factory);
  }

  private static void resetConfigService() throws Exception {
    Method resetMethod = ConfigService.class.getDeclaredMethod("reset");
    resetMethod.setAccessible(true);
    resetMethod.invoke(null);
  }

  private static void clearApolloCaches() throws Exception {
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configs");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configLocks");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFiles");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFileLocks");
    clearField(ApolloInjector.getInstance(ConfigFactoryManager.class), "m_factories");
    clearField(ApolloInjector.getInstance(ConfigRegistry.class), "m_instances");
  }

  private static void clearField(Object instance, String fieldName) throws Exception {
    Field field = instance.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object value = field.get(instance);
    if (value == null) {
      return;
    }
    if (value instanceof Map) {
      ((Map<?, ?>) value).clear();
      return;
    }
    if (value instanceof Table) {
      ((Table<?, ?, ?>) value).clear();
    }
  }

  private static void restoreOrClear(String key, String originalValue) {
    if (originalValue == null) {
      System.clearProperty(key);
      return;
    }
    System.setProperty(key, originalValue);
  }

  private static class StaticConfigFactory implements ConfigFactory {

    private final String content;

    private StaticConfigFactory(String content) {
      this.content = content;
    }

    @Override
    public Config create(String namespace) {
      return null;
    }

    @Override
    public Config create(String appId, String namespace) {
      return null;
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
      return new StaticConfigFile(null, namespace, configFileFormat, content);
    }

    @Override
    public ConfigFile createConfigFile(String appId, String namespace,
        ConfigFileFormat configFileFormat) {
      return new StaticConfigFile(appId, namespace, configFileFormat, content);
    }
  }

  private static class StaticConfigFile implements ConfigFile {

    private final String appId;
    private final String namespace;
    private final ConfigFileFormat format;
    private final String content;

    private StaticConfigFile(String appId, String namespace, ConfigFileFormat format, String content) {
      this.appId = appId;
      this.namespace = namespace;
      this.format = format;
      this.content = content;
    }

    @Override
    public String getContent() {
      return content;
    }

    @Override
    public boolean hasContent() {
      return content != null && !content.isEmpty();
    }

    @Override
    public String getAppId() {
      return appId;
    }

    @Override
    public String getNamespace() {
      return namespace;
    }

    @Override
    public ConfigFileFormat getConfigFileFormat() {
      return format;
    }

    @Override
    public void addChangeListener(ConfigFileChangeListener listener) {
    }

    @Override
    public boolean removeChangeListener(ConfigFileChangeListener listener) {
      return false;
    }

    @Override
    public ConfigSourceType getSourceType() {
      return ConfigSourceType.REMOTE;
    }
  }
}
