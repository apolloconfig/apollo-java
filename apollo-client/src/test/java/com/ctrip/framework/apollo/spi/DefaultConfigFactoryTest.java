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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.internals.PropertiesCompatibleFileConfigRepository;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.internals.JsonConfigFile;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.PropertiesConfigFile;
import com.ctrip.framework.apollo.internals.XmlConfigFile;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.internals.YmlConfigFile;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactoryTest {
  private DefaultConfigFactory defaultConfigFactory;
  private static String someAppId;
  private static Env someEnv;

  @Before
  public void setUp() throws Exception {
    someAppId = "someId";
    someEnv = Env.DEV;
    MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
    defaultConfigFactory = spy(new DefaultConfigFactory());
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testCreate() throws Exception {
    String someNamespace = "someName";
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    LocalFileConfigRepository someLocalConfigRepo = mock(LocalFileConfigRepository.class);
    when(someLocalConfigRepo.getConfig()).thenReturn(someProperties);

    doReturn(someLocalConfigRepo).when(defaultConfigFactory).createConfigRepository(someAppId, someNamespace);

    Config result = defaultConfigFactory.create(someNamespace);

    assertThat("DefaultConfigFactory should create DefaultConfig", result,
        is(instanceOf(DefaultConfig.class)));
    assertEquals(someValue, result.getProperty(someKey, null));
  }

  @Test
  public void testCreateLocalConfigRepositoryInLocalDev() throws Exception {
    String someNamespace = "someName";
    someEnv = Env.LOCAL;

    LocalFileConfigRepository localFileConfigRepository =
        defaultConfigFactory.createLocalConfigRepository(someAppId, someNamespace);

    assertNull(ReflectionTestUtils.getField(localFileConfigRepository, "m_upstream"));
  }

  @Test
  public void testCreatePropertiesCompatibleFileConfigRepository() throws Exception {
    ConfigFileFormat somePropertiesCompatibleFormat = ConfigFileFormat.YML;
    String someNamespace = "someName" + "." + somePropertiesCompatibleFormat;
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    PropertiesCompatibleFileConfigRepository someRepository = mock(PropertiesCompatibleFileConfigRepository.class);
    when(someRepository.getConfig()).thenReturn(someProperties);

    doReturn(someRepository).when(defaultConfigFactory)
        .createPropertiesCompatibleFileConfigRepository(someAppId, someNamespace, somePropertiesCompatibleFormat);

    Config result = defaultConfigFactory.create(someAppId, someNamespace);

    assertThat("DefaultConfigFactory should create DefaultConfig", result,
        is(instanceOf(DefaultConfig.class)));
    assertEquals(someValue, result.getProperty(someKey, null));
  }

  @Test
  public void testCreateConfigFile() throws Exception {
    String someNamespace = "someName";
    String anotherNamespace = "anotherName";
    String yetAnotherNamespace = "yetAnotherNamespace";
    Properties someProperties = new Properties();

    LocalFileConfigRepository someLocalConfigRepo = mock(LocalFileConfigRepository.class);
    when(someLocalConfigRepo.getConfig()).thenReturn(someProperties);

    doReturn(someLocalConfigRepo).when(defaultConfigFactory).createLocalConfigRepository(someAppId, someNamespace);
    doReturn(someLocalConfigRepo).when(defaultConfigFactory).createLocalConfigRepository(someAppId, anotherNamespace);
    doReturn(someLocalConfigRepo).when(defaultConfigFactory).createLocalConfigRepository(someAppId, yetAnotherNamespace);

    ConfigFile propertyConfigFile =
        defaultConfigFactory.createConfigFile(someNamespace, ConfigFileFormat.Properties);
    ConfigFile xmlConfigFile =
        defaultConfigFactory.createConfigFile(anotherNamespace, ConfigFileFormat.XML);
    ConfigFile jsonConfigFile =
        defaultConfigFactory.createConfigFile(yetAnotherNamespace, ConfigFileFormat.JSON);
    ConfigFile ymlConfigFile = defaultConfigFactory.createConfigFile(someNamespace,
        ConfigFileFormat.YML);
    ConfigFile yamlConfigFile = defaultConfigFactory.createConfigFile(someNamespace,
        ConfigFileFormat.YAML);

    assertThat("Should create PropertiesConfigFile for properties format", propertyConfigFile, is(instanceOf(
        PropertiesConfigFile.class)));
    assertEquals(someNamespace, propertyConfigFile.getNamespace());

    assertThat("Should create XmlConfigFile for xml format", xmlConfigFile, is(instanceOf(
        XmlConfigFile.class)));
    assertEquals(anotherNamespace, xmlConfigFile.getNamespace());

    assertThat("Should create JsonConfigFile for json format", jsonConfigFile, is(instanceOf(
        JsonConfigFile.class)));
    assertEquals(yetAnotherNamespace, jsonConfigFile.getNamespace());

    assertThat("Should create YmlConfigFile for yml format", ymlConfigFile, is(instanceOf(
        YmlConfigFile.class)));
    assertEquals(someNamespace, ymlConfigFile.getNamespace());

    assertThat("Should create YamlConfigFile for yaml format", yamlConfigFile, is(instanceOf(
        YamlConfigFile.class)));
    assertEquals(someNamespace, yamlConfigFile.getNamespace());

  }

  @Test
  public void testDetermineFileFormat() throws Exception {
    checkFileFormat("abc", ConfigFileFormat.Properties);
    checkFileFormat("abc.properties", ConfigFileFormat.Properties);
    checkFileFormat("abc.pRopErties", ConfigFileFormat.Properties);
    checkFileFormat("abc.xml", ConfigFileFormat.XML);
    checkFileFormat("abc.xmL", ConfigFileFormat.XML);
    checkFileFormat("abc.json", ConfigFileFormat.JSON);
    checkFileFormat("abc.jsOn", ConfigFileFormat.JSON);
    checkFileFormat("abc.yaml", ConfigFileFormat.YAML);
    checkFileFormat("abc.yAml", ConfigFileFormat.YAML);
    checkFileFormat("abc.yml", ConfigFileFormat.YML);
    checkFileFormat("abc.yMl", ConfigFileFormat.YML);
    checkFileFormat("abc.properties.yml", ConfigFileFormat.YML);
  }

  @Test
  public void testTrimNamespaceFormat() throws Exception {
    checkNamespaceName("abc", ConfigFileFormat.Properties, appIdAndNamespace("abc"));
    checkNamespaceName("abc.properties", ConfigFileFormat.Properties, appIdAndNamespace("abc"));
    checkNamespaceName("abcproperties", ConfigFileFormat.Properties, appIdAndNamespace("abcproperties"));
    checkNamespaceName("abc.pRopErties", ConfigFileFormat.Properties, appIdAndNamespace("abc"));
    checkNamespaceName("abc.xml", ConfigFileFormat.XML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.xmL", ConfigFileFormat.XML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.json", ConfigFileFormat.JSON, appIdAndNamespace("abc"));
    checkNamespaceName("abc.jsOn", ConfigFileFormat.JSON, appIdAndNamespace("abc"));
    checkNamespaceName("abc.yaml", ConfigFileFormat.YAML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.yAml", ConfigFileFormat.YAML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.yml", ConfigFileFormat.YML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.yMl", ConfigFileFormat.YML, appIdAndNamespace("abc"));
    checkNamespaceName("abc.proPerties.yml", ConfigFileFormat.YML, appIdAndNamespace("abc.proPerties"));
  }

  private String appIdAndNamespace(String namespace){
    return namespace;
  }


  private void checkFileFormat(String namespaceName, ConfigFileFormat expectedFormat) {
    assertEquals(expectedFormat, defaultConfigFactory.determineFileFormat(namespaceName));
  }

  private void checkNamespaceName(String namespaceName, ConfigFileFormat format, String expectedNamespaceName) {
    assertEquals(expectedNamespaceName, defaultConfigFactory.trimNamespaceFormat(namespaceName, format));
  }

  public static class MockConfigUtil extends ConfigUtil {
    @Override
    public String getAppId() {
      return someAppId;
    }

    @Override
    public Env getApolloEnv() {
      return someEnv;
    }
  }

}
