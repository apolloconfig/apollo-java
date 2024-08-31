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
package com.ctrip.framework.apollo.monitor.internal;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.impl.NullApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientBootstrapArgsApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientExceptionApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientNamespaceApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientThreadPoolApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.NullClientThreadPoolMonitorApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class ApolloClientMonitorContextTest {

  @Mock
  private DefaultApolloClientExceptionApi exceptionMonitorApi;
  @Mock
  private DefaultApolloClientNamespaceApi namespaceMonitorApi;
  @Mock
  private DefaultApolloClientBootstrapArgsApi bootstrapArgsMonitorApi;
  @Mock
  private DefaultApolloClientThreadPoolApi threadPoolMonitorApi;
  @Mock
  private ApolloClientMetricsExporter metricsExporter;

  private ApolloClientMonitorContext monitorContext;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monitorContext = new ApolloClientMonitorContext();
  }

  @Test
  public void testInitContext(){
    assertTrue(monitorContext.getBootstrapArgsApi() instanceof NullClientBootstrapArgsMonitorApi);
    assertTrue(monitorContext.getNamespaceApi() instanceof NullClientNamespaceMonitorApi);
    assertTrue(monitorContext.getThreadPoolApi() instanceof NullClientThreadPoolMonitorApi);
    assertTrue(monitorContext.getExceptionApi() instanceof NullClientExceptionMonitorApi);
    assertTrue(monitorContext.getMetricsExporter() instanceof NullApolloClientMetricsExporter);
  }

  @Test
  public void testSettingAndGettingApis() {
    monitorContext.setApolloClientExceptionMonitorApi(exceptionMonitorApi);
    monitorContext.setApolloClientNamespaceMonitorApi(namespaceMonitorApi);
    monitorContext.setApolloClientBootstrapArgsMonitorApi(bootstrapArgsMonitorApi);
    monitorContext.setApolloClientThreadPoolMonitorApi(threadPoolMonitorApi);
    monitorContext.setApolloClientMetricsExporter(metricsExporter);

    assertSame(exceptionMonitorApi, monitorContext.getExceptionApi());
    assertSame(namespaceMonitorApi, monitorContext.getNamespaceApi());
    assertSame(bootstrapArgsMonitorApi, monitorContext.getBootstrapArgsApi());
    assertSame(threadPoolMonitorApi, monitorContext.getThreadPoolApi());
    assertSame(metricsExporter, monitorContext.getMetricsExporter());
  }

  @Test
  public void testGetCollectors() {
    monitorContext.setApolloClientExceptionMonitorApi(exceptionMonitorApi);
    monitorContext.setApolloClientNamespaceMonitorApi(namespaceMonitorApi);
    monitorContext.setApolloClientBootstrapArgsMonitorApi(bootstrapArgsMonitorApi);
    monitorContext.setApolloClientThreadPoolMonitorApi(threadPoolMonitorApi);

    List<ApolloClientMonitorEventListener> collectors = monitorContext.getCollectors();

    assertEquals(4, collectors.size());
    assertTrue(collectors.contains(exceptionMonitorApi));
    assertTrue(collectors.contains(namespaceMonitorApi));
    assertTrue(collectors.contains(bootstrapArgsMonitorApi));
    assertTrue(collectors.contains(threadPoolMonitorApi));
  }
}
