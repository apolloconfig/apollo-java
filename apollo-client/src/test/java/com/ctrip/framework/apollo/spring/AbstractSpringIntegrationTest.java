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
package com.ctrip.framework.apollo.spring;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.DefaultInjector;
import com.ctrip.framework.apollo.internals.SimpleConfig;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import java.util.Objects;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.springframework.util.ReflectionUtils;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class AbstractSpringIntegrationTest {
  private static final Table<String, String, Config> CONFIG_REGISTRY = HashBasedTable.create();
  private static final Map<String, ConfigFile> CONFIG_FILE_REGISTRY = Maps.newHashMap();
  private static Method CONFIG_SERVICE_RESET;
  private static Method PROPERTY_SOURCES_PROCESSOR_RESET;

  protected static String someAppId = "someAppId";

  static {
    try {
      CONFIG_SERVICE_RESET = ConfigService.class.getDeclaredMethod("reset");
      ReflectionUtils.makeAccessible(CONFIG_SERVICE_RESET);
      PROPERTY_SOURCES_PROCESSOR_RESET = PropertySourcesProcessor.class.getDeclaredMethod("reset");
      ReflectionUtils.makeAccessible(PROPERTY_SOURCES_PROCESSOR_RESET);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setUp() throws Exception {
    doSetUp();
  }

  @After
  public void tearDown() throws Exception {
    doTearDown();
  }

  protected SimpleConfig prepareConfig(String appId, String namespaceName, Properties properties) {
    ConfigRepository configRepository = mock(ConfigRepository.class);

    when(configRepository.getConfig()).thenReturn(properties);

    SimpleConfig config = new SimpleConfig(appId, namespaceName, configRepository);

    mockConfig(appId, namespaceName, config);

    return config;
  }

  protected static Properties readYamlContentAsConfigFileProperties(String caseName)
      throws IOException {
    final String filePath = "spring/yaml/" + caseName;
    ClassLoader classLoader = AbstractSpringIntegrationTest.class.getClassLoader();

    InputStream inputStream = classLoader.getResourceAsStream(filePath);
    Objects.requireNonNull(inputStream, filePath + " may be not exist under src/test/resources/");
    String yamlContent = CharStreams
        .toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

    Properties properties = new Properties();
    properties.setProperty(ConfigConsts.CONFIG_FILE_CONTENT_KEY, yamlContent);

    return properties;
  }

  protected static YamlConfigFile prepareYamlConfigFile(String appId, String namespaceNameWithFormat, Properties properties) {
    ConfigRepository configRepository = mock(ConfigRepository.class);

    when(configRepository.getConfig()).thenReturn(properties);

    // spy it for testing after
    YamlConfigFile configFile = spy(new YamlConfigFile(appId, namespaceNameWithFormat, configRepository));

    mockConfigFile(appId, namespaceNameWithFormat, configFile);

    return configFile;
  }

  protected Properties assembleProperties(String key, String value) {
    Properties properties = new Properties();
    properties.setProperty(key, value);

    return properties;
  }

  protected Properties assembleProperties(String key, String value, String key2, String value2) {
    Properties properties = new Properties();
    properties.setProperty(key, value);
    properties.setProperty(key2, value2);

    return properties;
  }

  protected Properties assembleProperties(String key, String value, String key2, String value2,
      String key3, String value3) {

    Properties properties = new Properties();
    properties.setProperty(key, value);
    properties.setProperty(key2, value2);
    properties.setProperty(key3, value3);

    return properties;
  }

  protected Date assembleDate(int year, int month, int day, int hour, int minute, int second, int millisecond) {
    Calendar date = Calendar.getInstance();
    date.set(year, month - 1, day, hour, minute, second); //Month in Calendar is 0 based
    date.set(Calendar.MILLISECOND, millisecond);

    return date.getTime();
  }


  protected static void mockConfig(String appId, String namespace, Config config) {
    CONFIG_REGISTRY.put(appId, namespace, config);
  }

  protected static void mockConfigFile(String namespaceNameWithFormat, ConfigFile configFile) {
    CONFIG_FILE_REGISTRY.put(namespaceNameWithFormat, configFile);
  }

  protected static void mockConfigFile(String appId,String namespaceNameWithFormat, ConfigFile configFile) {
    CONFIG_FILE_REGISTRY.put(appId + "+" + namespaceNameWithFormat, configFile);
  }

  protected static void doSetUp() throws Exception {
    //as ConfigService is singleton, so we must manually clear its container
    ReflectionUtils.invokeMethod(CONFIG_SERVICE_RESET, null);
    //as PropertySourcesProcessor has some static variables, so we must manually clear them
    ReflectionUtils.invokeMethod(PROPERTY_SOURCES_PROCESSOR_RESET, null);

    MockConfigUtil configUtil = new MockConfigUtil();
    configUtil.setAutoUpdateInjectedSpringProperties(true);
    MockInjector.setInstance(ConfigUtil.class, configUtil);

    DefaultInjector defaultInjector = new DefaultInjector();
    ConfigManager defaultConfigManager = defaultInjector.getInstance(ConfigManager.class);
    MockInjector.setInstance(ConfigManager.class, new MockConfigManager(defaultConfigManager));
    MockInjector.setDelegate(defaultInjector);

  }

  protected static void doTearDown() {
    MockInjector.reset();
    CONFIG_REGISTRY.clear();
    CONFIG_FILE_REGISTRY.clear();
  }

  private static class MockConfigManager implements ConfigManager {

    private final ConfigManager delegate;

    private final String defaultAppId = someAppId;

    public MockConfigManager(ConfigManager delegate) {
      this.delegate = delegate;
    }

    @Override
    public Config getConfig(String namespace) {
      return getConfig(defaultAppId, namespace);
    }

    @Override
    public Config getConfig(String appId, String namespace) {
      Config config = CONFIG_REGISTRY.get(appId, namespace);
      if (config != null) {
        return config;
      }
      return delegate.getConfig(appId, namespace);
    }

    @Override
    public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
      return this.getConfigFile(defaultAppId, namespace, configFileFormat);
    }

    @Override
    public ConfigFile getConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
      ConfigFile configFile = CONFIG_FILE_REGISTRY.get(String.format("%s+%s.%s", appId, namespace, configFileFormat.getValue()));
      if (configFile != null) {
        return configFile;
      }
      return delegate.getConfigFile(namespace, configFileFormat);
    }
  }

  protected static class MockConfigUtil extends ConfigUtil {

    private boolean isAutoUpdateInjectedSpringProperties;

    public void setAutoUpdateInjectedSpringProperties(boolean autoUpdateInjectedSpringProperties) {
      isAutoUpdateInjectedSpringProperties = autoUpdateInjectedSpringProperties;
    }

    @Override
    public boolean isAutoUpdateInjectedSpringPropertiesEnabled() {
      return isAutoUpdateInjectedSpringProperties;
    }

    public String getAppId() {
      return someAppId;
    }
  }
}
