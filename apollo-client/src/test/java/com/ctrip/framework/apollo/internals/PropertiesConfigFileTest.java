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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Properties;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import org.mockito.stubbing.Answer;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertiesConfigFileTest {

  private String someAppId;
  private String someNamespace;
  @Mock
  private ConfigRepository configRepository;
  @Mock
  private PropertiesFactory propertiesFactory;

  @Before
  public void setUp() throws Exception {
    someAppId = "someAppId";
    someNamespace = "someName";
    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new Properties();
      }
    });
    MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testWhenHasContent() throws Exception {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    when(configRepository.getConfig()).thenReturn(someProperties);

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertEquals(ConfigFileFormat.Properties, configFile.getConfigFileFormat());
    assertEquals(someNamespace, configFile.getNamespace());
    assertTrue(configFile.hasContent());
    assertTrue(configFile.getContent().contains(String.format("%s=%s", someKey, someValue)));
  }

  @Test
  public void testWhenHasNoContent() throws Exception {
    when(configRepository.getConfig()).thenReturn(null);

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());
  }

  @Test
  public void testWhenConfigRepositoryHasError() throws Exception {
    when(configRepository.getConfig()).thenThrow(new RuntimeException("someError"));

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());
  }

  @Test
  public void testOnRepositoryChange() throws Exception {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    String anotherValue = "anotherValue";
    someProperties.setProperty(someKey, someValue);

    when(configRepository.getConfig()).thenReturn(someProperties);

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertTrue(configFile.getContent().contains(String.format("%s=%s", someKey, someValue)));

    Properties anotherProperties = new Properties();
    anotherProperties.setProperty(someKey, anotherValue);

    final SettableFuture<ConfigFileChangeEvent> configFileChangeFuture = SettableFuture.create();
    ConfigFileChangeListener someListener = new ConfigFileChangeListener() {
      @Override
      public void onChange(ConfigFileChangeEvent changeEvent) {
        configFileChangeFuture.set(changeEvent);
      }
    };

    configFile.addChangeListener(someListener);

    configFile.onRepositoryChange(someNamespace, anotherProperties);

    ConfigFileChangeEvent changeEvent = configFileChangeFuture.get(500, TimeUnit.MILLISECONDS);

    assertFalse(configFile.getContent().contains(String.format("%s=%s", someKey, someValue)));
    assertTrue(configFile.getContent().contains(String.format("%s=%s", someKey, anotherValue)));

    assertEquals(someNamespace, changeEvent.getNamespace());
    assertTrue(changeEvent.getOldValue().contains(String.format("%s=%s", someKey, someValue)));
    assertTrue(changeEvent.getNewValue().contains(String.format("%s=%s", someKey, anotherValue)));
    assertEquals(PropertyChangeType.MODIFIED, changeEvent.getChangeType());
  }

  @Test
  public void testWhenConfigRepositoryHasErrorAndThenRecovered() throws Exception {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    when(configRepository.getConfig()).thenThrow(new RuntimeException("someError"));

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertFalse(configFile.hasContent());
    assertNull(configFile.getContent());

    configFile.onRepositoryChange(someNamespace, someProperties);

    assertTrue(configFile.hasContent());
    assertTrue(configFile.getContent().contains(String.format("%s=%s", someKey, someValue)));
  }

  @Test
  public void testIfCompatibleWithProperties() {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    when(configRepository.getConfig()).thenReturn(someProperties);

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertEquals(configFile.asProperties(),someProperties);
    assertEquals(ConfigFileFormat.Properties, configFile.getConfigFileFormat());
    assertEquals(someNamespace, configFile.getNamespace());
    assertTrue(configFile.hasContent());
    assertTrue(configFile.getContent().contains(String.format("%s=%s", someKey, someValue)));
  }

  @Test
  public void testIfCompatibleWithEmptyProperties() {
    Properties someProperties = new Properties();

    when(configRepository.getConfig()).thenReturn(someProperties);

    PropertiesConfigFile configFile = new PropertiesConfigFile(someAppId, someNamespace, configRepository);

    assertEquals(configFile.asProperties(),someProperties);
    assertEquals(ConfigFileFormat.Properties, configFile.getConfigFileFormat());
    assertEquals(someNamespace, configFile.getNamespace());
    assertFalse(configFile.hasContent());

  }
}
