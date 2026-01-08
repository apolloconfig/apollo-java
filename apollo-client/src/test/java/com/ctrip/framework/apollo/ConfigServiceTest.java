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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.MetaDomainConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.internals.AbstractConfig;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigServiceTest {
  private static String someAppId;

  @BeforeEach
  public void setUp() throws Exception {
    someAppId = "someAppId";

    MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
  }

  @AfterEach
  public void tearDown() throws Exception {
    //as ConfigService is singleton, so we must manually clear its container
    ConfigService.reset();
    MockInjector.reset();
    ReflectionTestUtils.invokeMethod(MetaDomainConsts.class, "reset");
  }

  @Test
  public void testHackConfig() {
    String someNamespace = "hack";
    String someKey = "first";
    ConfigService.setConfig(new MockConfig(someAppId, someNamespace));

    Config config = ConfigService.getAppConfig();

    assertEquals(someAppId + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + someNamespace + ":" + someKey, config.getProperty(someKey, null));
      assertNull(config.getProperty("unknown", null));
  }

  @Test
  public void testHackConfigFactory() throws Exception {
    String someKey = "someKey";
    ConfigService.setConfigFactory(new MockConfigFactory());

    Config config = ConfigService.getAppConfig();

    assertEquals(someAppId + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + ConfigConsts.NAMESPACE_APPLICATION + ":" + someKey,
        config.getProperty(someKey, null));
  }

  @Test
  public void testMockConfigFactory() throws Exception {
    String someNamespace = "mock";
    String someKey = "someKey";
    MockInjector.setInstance(ConfigFactory.class, someNamespace, new MockConfigFactory());

    Config config = ConfigService.getConfig(someNamespace);

    assertEquals(someAppId + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + someNamespace + ":" + someKey, config.getProperty(someKey, null));
      assertNull(config.getProperty("unknown", null));
  }

  @Test
  public void testMockConfigFactoryForConfigFile() throws Exception {
    String someNamespace = "mock";
    ConfigFileFormat someConfigFileFormat = ConfigFileFormat.Properties;
    String someNamespaceFileName =
        String.format("%s.%s", someNamespace, someConfigFileFormat.getValue());
    MockInjector.setInstance(ConfigFactory.class, someNamespaceFileName, new MockConfigFactory());
    ConfigFile configFile = ConfigService.getConfigFile(someNamespace, someConfigFileFormat);

    assertEquals(someNamespaceFileName, configFile.getNamespace());
    assertEquals(someNamespaceFileName + ":" + someConfigFileFormat.getValue(), configFile.getContent());
  }

  private static class MockConfig extends AbstractConfig {
    private final String m_appId;
    private final String m_namespace;

    public MockConfig(String appId, String namespace) {
      m_appId = appId;
      m_namespace = namespace;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
      if (key.equals("unknown")) {
        return null;
      }

      return m_appId + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + m_namespace + ":" + key;
    }

    @Override
    public Set<String> getPropertyNames() {
      return null;
    }

    @Override
    public ConfigSourceType getSourceType() {
      return null;
    }
  }

  private static class MockConfigFile implements ConfigFile {
    private final ConfigFileFormat m_configFileFormat;
    private String m_appId;
    private final String m_namespace;

    public MockConfigFile(String namespace,
                          ConfigFileFormat configFileFormat) {
      m_namespace = namespace;
      m_configFileFormat = configFileFormat;
    }

    public MockConfigFile(String appId, String namespace,
                          ConfigFileFormat configFileFormat) {
      m_appId = appId;
      m_namespace = namespace;
      m_configFileFormat = configFileFormat;
    }

    @Override
    public String getContent() {
      return m_namespace + ":" + m_configFileFormat.getValue();
    }

    @Override
    public boolean hasContent() {
      return true;
    }

    @Override
    public String getAppId() {
      return m_appId;
    }

    @Override
    public String getNamespace() {
      return m_namespace;
    }

    @Override
    public ConfigFileFormat getConfigFileFormat() {
      return m_configFileFormat;
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
      return null;
    }
  }

  public static class MockConfigFactory implements ConfigFactory {
    @Override
    public Config create(String namespace) {
      return this.create(someAppId, namespace);
    }

    @Override
    public Config create(String appId, String namespace) {
      return new MockConfig(appId, namespace);
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
      return createConfigFile(someAppId, namespace, configFileFormat);
    }

    @Override
    public ConfigFile createConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat) {
      return new MockConfigFile(appId, namespace, configFileFormat);
    }
  }

  public static class MockConfigUtil extends ConfigUtil {
    @Override
    public String getAppId() {
      return someAppId;
    }
  }

}
