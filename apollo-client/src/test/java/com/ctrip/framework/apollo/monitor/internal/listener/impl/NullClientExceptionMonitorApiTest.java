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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class NullClientExceptionMonitorApiTest {

  private NullClientExceptionMonitorApi exceptionMonitorApi;

  @BeforeEach
  public void setUp() {
    exceptionMonitorApi = new NullClientExceptionMonitorApi();
  }

  @Test
  public void testGetApolloConfigExceptionList() {
    List<Exception> exceptionList = exceptionMonitorApi.getApolloConfigExceptionList();

    assertNotNull(exceptionList);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  public void testGetApolloConfigExceptionDetails() {
    List<String> exceptionDetails = exceptionMonitorApi.getApolloConfigExceptionDetails();

    assertNotNull(exceptionDetails);
    assertTrue(exceptionDetails.isEmpty());
  }
}
