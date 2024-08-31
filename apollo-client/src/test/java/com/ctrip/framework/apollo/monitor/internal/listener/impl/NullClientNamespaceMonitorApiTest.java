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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import static org.junit.Assert.*;

import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi.NamespaceMetrics;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class NullClientNamespaceMonitorApiTest {

  private NullClientNamespaceMonitorApi namespaceMonitorApi;

  @Before
  public void setUp() {
    namespaceMonitorApi = new NullClientNamespaceMonitorApi();
  }

  @Test
  public void testGetNamespaceMetrics() {
    Map<String, NamespaceMetrics> metrics = namespaceMonitorApi.getNamespaceMetrics();

    assertNotNull(metrics);
    assertTrue(metrics.isEmpty());
  }

  @Test
  public void testGetNamespaceItemNames() {
    Integer testNamespace = namespaceMonitorApi.getNamespacePropertySize("testNamespace");
    assertEquals(0, testNamespace.intValue());

  }

  @Test
  public void testGetConfigFileNamespaces() {
    List<String> configFileNamespaces = namespaceMonitorApi.getConfigFileNamespaces();
    assertEquals(0, configFileNamespaces.size());
  }

  @Test
  public void testGetNotFoundNamespaces() {
    List<String> notFoundNamespaces = namespaceMonitorApi.getNotFoundNamespaces();

    assertNotNull(notFoundNamespaces);
    assertTrue(notFoundNamespaces.isEmpty());
  }

  @Test
  public void testGetTimeoutNamespaces() {
    List<String> timeoutNamespaces = namespaceMonitorApi.getTimeoutNamespaces();

    assertNotNull(timeoutNamespaces);
    assertTrue(timeoutNamespaces.isEmpty());
  }
}
