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

import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi.ApolloThreadPoolInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NullClientThreadPoolMonitorApiTest {

  private NullClientThreadPoolMonitorApi monitorApi;

  @BeforeEach
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
    assertNotNull(info);
    assertEquals(0, info.getPoolSize());
  }
  
}
