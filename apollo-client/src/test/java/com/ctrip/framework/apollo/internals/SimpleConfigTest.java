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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@ExtendWith(MockitoExtension.class)
public class SimpleConfigTest {

  private String someAppId;
  private String someNamespace;
  @Mock
  private ConfigRepository configRepository;
  @Mock
  private PropertiesFactory propertiesFactory;
  private ConfigSourceType someSourceType;

  @BeforeEach
  public void setUp() throws Exception {
    someAppId = "someAppId";
    someNamespace = "someName";

      lenient().when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new Properties();
      }
    });
    MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);
  }

  @AfterEach
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testGetProperty() throws Exception {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    someProperties.setProperty(someKey, someValue);

    someSourceType = ConfigSourceType.LOCAL;

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);

    SimpleConfig config = new SimpleConfig(someAppId, someNamespace, configRepository);

    assertEquals(someValue, config.getProperty(someKey, null));
    assertEquals(someSourceType, config.getSourceType());
  }

  @Test
  public void testLoadConfigFromConfigRepositoryError() throws Exception {
    String someKey = "someKey";
    String anyValue = "anyValue" + Math.random();

    when(configRepository.getConfig()).thenThrow(mock(RuntimeException.class));

    Config config = new SimpleConfig(someAppId, someNamespace, configRepository);

    assertEquals(anyValue, config.getProperty(someKey, anyValue));
    assertEquals(ConfigSourceType.NONE, config.getSourceType());
  }

  @Test
  public void testOnRepositoryChange() throws Exception {
    Properties someProperties = new Properties();
    String someKey = "someKey";
    String someValue = "someValue";
    String anotherKey = "anotherKey";
    String anotherValue = "anotherValue";
    someProperties.putAll(ImmutableMap.of(someKey, someValue, anotherKey, anotherValue));

    Properties anotherProperties = new Properties();
    String newKey = "newKey";
    String newValue = "newValue";
    String someValueNew = "someValueNew";
    anotherProperties.putAll(ImmutableMap.of(someKey, someValueNew, newKey, newValue));

    someSourceType = ConfigSourceType.LOCAL;

    when(configRepository.getConfig()).thenReturn(someProperties);
    when(configRepository.getSourceType()).thenReturn(someSourceType);

    final SettableFuture<ConfigChangeEvent> configChangeFuture = SettableFuture.create();
    ConfigChangeListener someListener = new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        configChangeFuture.set(changeEvent);
      }
    };

    SimpleConfig config = new SimpleConfig(someAppId, someNamespace, configRepository);

    assertEquals(someSourceType, config.getSourceType());

    config.addChangeListener(someListener);

    ConfigSourceType anotherSourceType = ConfigSourceType.REMOTE;
    when(configRepository.getSourceType()).thenReturn(anotherSourceType);

    config.onRepositoryChange(someAppId, someNamespace, anotherProperties);

    ConfigChangeEvent changeEvent = configChangeFuture.get(500, TimeUnit.MILLISECONDS);

    assertEquals(someAppId, changeEvent.getAppId());
    assertEquals(someNamespace, changeEvent.getNamespace());
    assertEquals(3, changeEvent.changedKeys().size());

    ConfigChange someKeyChange = changeEvent.getChange(someKey);
    assertEquals(someValue, someKeyChange.getOldValue());
    assertEquals(someValueNew, someKeyChange.getNewValue());
    assertEquals(PropertyChangeType.MODIFIED, someKeyChange.getChangeType());

    ConfigChange anotherKeyChange = changeEvent.getChange(anotherKey);
    assertEquals(anotherValue, anotherKeyChange.getOldValue());
    assertEquals(null, anotherKeyChange.getNewValue());
    assertEquals(PropertyChangeType.DELETED, anotherKeyChange.getChangeType());

    ConfigChange newKeyChange = changeEvent.getChange(newKey);
    assertEquals(null, newKeyChange.getOldValue());
    assertEquals(newValue, newKeyChange.getNewValue());
    assertEquals(PropertyChangeType.ADDED, newKeyChange.getChangeType());

    assertEquals(anotherSourceType, config.getSourceType());
  }
}
