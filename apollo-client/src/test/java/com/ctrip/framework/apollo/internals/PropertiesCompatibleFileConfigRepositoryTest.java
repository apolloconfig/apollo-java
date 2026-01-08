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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PropertiesCompatibleFileConfigRepositoryTest {

  @Mock
  private PropertiesCompatibleConfigFile configFile;

  private String someNamespaceName;
  private String someAppId;

  @Mock
  private Properties someProperties;

  @BeforeEach
  public void setUp() throws Exception {
    someNamespaceName = "someNamespaceName";
    someAppId = "someAppId";
    lenient().when(configFile.getNamespace()).thenReturn(someNamespaceName);
      lenient().when(configFile.getAppId()).thenReturn(someAppId);
      lenient().when(configFile.asProperties()).thenReturn(someProperties);
  }

  @Test
  public void testGetConfig() throws Exception {
    PropertiesCompatibleFileConfigRepository configFileRepository = new PropertiesCompatibleFileConfigRepository(
        configFile);

    assertSame(someProperties, configFileRepository.getConfig());
    verify(configFile, times(1)).addChangeListener(configFileRepository);
  }

  @Test
  public void testGetConfigFailedAndThenRecovered() throws Exception {
    RuntimeException someException = new RuntimeException("some exception");

    when(configFile.asProperties()).thenThrow(someException);

    PropertiesCompatibleFileConfigRepository configFileRepository = new PropertiesCompatibleFileConfigRepository(
        configFile);

    Throwable exceptionThrown = null;
    try {
      configFileRepository.getConfig();
    } catch (Throwable ex) {
      exceptionThrown = ex;
    }

    assertSame(someException, exceptionThrown);

    // recovered
    reset(configFile);

    Properties someProperties = mock(Properties.class);

    when(configFile.asProperties()).thenReturn(someProperties);

    assertSame(someProperties, configFileRepository.getConfig());
  }

  @Test
  public void testGetConfigWithConfigFileReturnNullProperties() throws Exception {
    when(configFile.asProperties()).thenReturn(null);

    PropertiesCompatibleFileConfigRepository configFileRepository = new PropertiesCompatibleFileConfigRepository(
        configFile);

      assertThrows(IllegalStateException.class,()->
    configFileRepository.getConfig());
  }

  @Test
  public void testGetSourceType() throws Exception {
    ConfigSourceType someType = ConfigSourceType.REMOTE;

    when(configFile.getSourceType()).thenReturn(someType);

    PropertiesCompatibleFileConfigRepository configFileRepository = new PropertiesCompatibleFileConfigRepository(
        configFile);

    assertSame(someType, configFileRepository.getSourceType());
  }

  @Test
  public void testOnChange() throws Exception {
    Properties anotherProperties = mock(Properties.class);
    ConfigFileChangeEvent someChangeEvent = mock(ConfigFileChangeEvent.class);

    RepositoryChangeListener someListener = mock(RepositoryChangeListener.class);

    PropertiesCompatibleFileConfigRepository configFileRepository = new PropertiesCompatibleFileConfigRepository(
        configFile);

    configFileRepository.addChangeListener(someListener);

    assertSame(someProperties, configFileRepository.getConfig());

    when(configFile.asProperties()).thenReturn(anotherProperties);

    configFileRepository.onChange(someChangeEvent);

    assertSame(anotherProperties, configFileRepository.getConfig());
    verify(someListener, times(1)).onRepositoryChange(someAppId, someNamespaceName, anotherProperties);
  }
}
