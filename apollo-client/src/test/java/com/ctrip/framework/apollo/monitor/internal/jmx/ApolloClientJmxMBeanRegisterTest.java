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
package com.ctrip.framework.apollo.monitor.internal.jmx;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ApolloClientJmxMBeanRegisterTest {

  private MBeanServer mockMBeanServer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockMBeanServer = mock(MBeanServer.class);
    ApolloClientJmxMBeanRegister.setMBeanServer(mockMBeanServer);
  }

  @Test
  public void testRegister_MBeanNotRegistered() throws Exception {
    String name = "com.example:type=TestMBean";
    Object mbean = new Object();

    when(mockMBeanServer.isRegistered(any(ObjectName.class))).thenReturn(false);
    ObjectName objectName = ApolloClientJmxMBeanRegister.register(name, mbean);

    assertNotNull(objectName);
    verify(mockMBeanServer).registerMBean(mbean, objectName);
  }

  @Test
  public void testRegister_MBeanAlreadyRegistered() throws Exception {
    String name = "com.example:type=TestMBean";
    Object mbean = new Object();

    ObjectName objectName = new ObjectName(name);
    when(mockMBeanServer.isRegistered(objectName)).thenReturn(true);

    ApolloClientJmxMBeanRegister.register(name, mbean);

    verify(mockMBeanServer).unregisterMBean(objectName);
    verify(mockMBeanServer).registerMBean(mbean, objectName);
  }

}
