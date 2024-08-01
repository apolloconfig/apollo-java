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
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientThreadPoolApi.ApolloThreadPoolInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class NullClientThreadPoolMonitorApiTest {

  private NullClientThreadPoolMonitorApi monitorApi;

  @Before
  public void setUp() {
    monitorApi = new NullClientThreadPoolMonitorApi();
  }

  @Test
  public void testGetThreadPoolInfo() {
    Map<String, ApolloThreadPoolInfo> threadPoolInfo = monitorApi.getThreadPoolInfo();

    assertNotNull(threadPoolInfo);
    assertTrue(threadPoolInfo.isEmpty());
  }

  @Test
  public void testGetRemoteConfigRepositoryThreadPoolInfo() {
    ApolloThreadPoolInfo info = monitorApi.getRemoteConfigRepositoryThreadPoolInfo();

    assertNull(info);
  }

  @Test
  public void testGetAbstractConfigThreadPoolInfo() {
    ApolloThreadPoolInfo info = monitorApi.getAbstractConfigThreadPoolInfo();

    assertNull(info);
  }

  @Test
  public void testGetAbstractConfigFileThreadPoolInfo() {
    ApolloThreadPoolInfo info = monitorApi.getAbstractConfigFileThreadPoolInfo();

    assertNull(info);
  }
}
