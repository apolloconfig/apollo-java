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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultConfigFactoryFileCachePropertyTest {

  private DefaultConfigFactory configFactory;
  private ConfigUtil someConfigUtil;
  private String someAppId;
  private String someNamespace;

  @BeforeEach
  public void setUp() throws Exception {
    someAppId = "someAppId";
    someNamespace = "someNamespace";
    someConfigUtil = mock(ConfigUtil.class);
    MockInjector.setInstance(ConfigUtil.class, someConfigUtil);
    configFactory = spy(new DefaultConfigFactory());
  }

  @Test
  public void testCreateFileEnableConfigRepository() throws Exception {
    LocalFileConfigRepository someLocalConfigRepository = mock(LocalFileConfigRepository.class);
    when(someConfigUtil.isPropertyFileCacheEnabled()).thenReturn(true);
    doReturn(someLocalConfigRepository).when(configFactory)
        .createLocalConfigRepository(someAppId, someNamespace);
    ConfigRepository configRepository = configFactory.createConfigRepository(someAppId, someNamespace);
    assertSame(someLocalConfigRepository, configRepository);
    verify(configFactory, times(1)).createLocalConfigRepository(someAppId, someNamespace);
    verify(configFactory, never()).createRemoteConfigRepository(someAppId, someNamespace);
  }

  @Test
  public void testCreateFileDisableConfigRepository() throws Exception {
    RemoteConfigRepository someRemoteConfigRepository = mock(RemoteConfigRepository.class);
    when(someConfigUtil.isPropertyFileCacheEnabled()).thenReturn(false);
    doReturn(someRemoteConfigRepository).when(configFactory)
        .createRemoteConfigRepository(someAppId, someNamespace);
    ConfigRepository configRepository = configFactory.createConfigRepository(someAppId, someNamespace);
    assertSame(someRemoteConfigRepository, configRepository);
    verify(configFactory, never()).createLocalConfigRepository(someAppId, someNamespace);
    verify(configFactory, times(1)).createRemoteConfigRepository(someAppId, someNamespace);
  }

  @AfterEach
  public void tearDown() throws Exception {
    MockInjector.reset();
  }
}
