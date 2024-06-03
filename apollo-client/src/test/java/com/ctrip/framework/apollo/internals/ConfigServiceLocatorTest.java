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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConfigServiceLocatorTest {

  @AfterEach
  public void tearDown() throws Exception {
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE);
    System.clearProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_CONFIG_SERVICE);
  }

  @Test
  public void testGetConfigServicesWithSystemProperty() throws Exception {
    String someConfigServiceUrl = " someConfigServiceUrl ";
    String anotherConfigServiceUrl = " anotherConfigServiceUrl ";

    System.setProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE, someConfigServiceUrl + "," + anotherConfigServiceUrl);

    ConfigServiceLocator configServiceLocator = new ConfigServiceLocator();

    List<ServiceDTO> result = configServiceLocator.getConfigServices();

    assertEquals(2, result.size());

    assertEquals(someConfigServiceUrl.trim(), result.get(0).getHomepageUrl());
    assertEquals(anotherConfigServiceUrl.trim(), result.get(1).getHomepageUrl());
  }

  @Test
  public void testGetConfigServicesWithSystemPropertyCompatible() throws Exception {
    String someConfigServiceUrl = " someConfigServiceUrl ";
    String anotherConfigServiceUrl = " anotherConfigServiceUrl ";

    System.setProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_CONFIG_SERVICE,
        someConfigServiceUrl + "," + anotherConfigServiceUrl);

    ConfigServiceLocator configServiceLocator = new ConfigServiceLocator();

    List<ServiceDTO> result = configServiceLocator.getConfigServices();

    assertEquals(2, result.size());

    assertEquals(someConfigServiceUrl.trim(), result.get(0).getHomepageUrl());
    assertEquals(anotherConfigServiceUrl.trim(), result.get(1).getHomepageUrl());
  }

  @Test
  public void giveNoUsableConfigServiceThenThrowExceptionQuickly() {
    ConfigServiceLocator configServiceLocator = Mockito.spy(
        new ConfigServiceLocator()
    );

    Mockito.doNothing().when(configServiceLocator).doSubmitUpdateTask();

    // trigger once
    assertThrows(ApolloConfigException.class, () -> {
      configServiceLocator.getConfigServices();
    });
    Mockito.verify(configServiceLocator, Mockito.times(1))
        .trySubmitUpdateTask();
    Mockito.verify(configServiceLocator, Mockito.times(1))
        .doSubmitUpdateTask();

    // trigger 9 times in a short period, but only submit a task
    for (int i = 0; i < 9; i++) {
      assertThrows(ApolloConfigException.class, () -> {
        configServiceLocator.getConfigServices();
      });
    }
    Mockito.verify(configServiceLocator, Mockito.times(10))
        .trySubmitUpdateTask();
    Mockito.verify(configServiceLocator, Mockito.times(1))
        .doSubmitUpdateTask();

    // clear queue
    {
      boolean oldValue = configServiceLocator.discoveryTaskQueueMark.getAndSet(false);
      assertTrue(oldValue);
    }

    // trigger 10 times in a short period, task will submit again
    for (int i = 0; i < 10; i++) {
      assertThrows(ApolloConfigException.class, () -> {
        configServiceLocator.getConfigServices();
      });
    }
    Mockito.verify(configServiceLocator, Mockito.times(20))
        .trySubmitUpdateTask();
    Mockito.verify(configServiceLocator, Mockito.times(2))
        .doSubmitUpdateTask();

  }

}
