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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.monitor.api.ApolloClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultConfigMonitorTest {

  @Mock
  private ApolloClientExceptionMonitorApi exceptionMonitorApi;
  @Mock
  private ApolloClientNamespaceMonitorApi namespaceMonitorApi;
  @Mock
  private ApolloClientBootstrapArgsMonitorApi bootstrapArgsMonitorApi;
  @Mock
  private ApolloClientThreadPoolMonitorApi threadPoolMonitorApi;
  @Mock
  private ApolloClientMetricsExporter metricsExporter;
  @Mock
  private ApolloClientMonitorContext monitorContext;

  private DefaultConfigMonitor configMonitor;

  @BeforeEach
  public void setUp(){
    MockitoAnnotations.initMocks(this);
    when(monitorContext.getExceptionApi()).thenReturn(exceptionMonitorApi);
    when(monitorContext.getNamespaceApi()).thenReturn(namespaceMonitorApi);
    when(monitorContext.getBootstrapArgsApi()).thenReturn(bootstrapArgsMonitorApi);
    when(monitorContext.getThreadPoolApi()).thenReturn(threadPoolMonitorApi);
    when(monitorContext.getMetricsExporter()).thenReturn(metricsExporter);
    MockInjector.setInstance(ApolloClientMonitorContext.class, monitorContext);
    
    configMonitor = new DefaultConfigMonitor();
  }

  @AfterEach
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testApis(){
    assertSame(exceptionMonitorApi, configMonitor.getExceptionMonitorApi());
    assertSame(namespaceMonitorApi, configMonitor.getNamespaceMonitorApi());
    assertSame(bootstrapArgsMonitorApi, configMonitor.getBootstrapArgsMonitorApi());
    assertSame(threadPoolMonitorApi, configMonitor.getThreadPoolMonitorApi());
  }

  @Test
  public void testExporterData(){
    String data = "data";
    when(metricsExporter.response()).thenReturn(data);

    assertEquals(data, configMonitor.getExporterData());
  }
}
