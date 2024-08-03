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

import com.ctrip.framework.apollo.monitor.api.ApolloClientBootstrapArgsMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientExceptionMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.api.ApolloClientThreadPoolMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultConfigMonitorTest {

    private DefaultConfigMonitor configMonitor;

    @Mock
    private ApolloClientMetricsExporter reporter;

    @Mock
    private ApolloClientThreadPoolMonitorApi threadPoolMonitorApi;

    @Mock
    private ApolloClientExceptionMonitorApi exceptionMonitorApi;

    @Mock
    private ApolloClientNamespaceMonitorApi namespaceMonitorApi;

    @Mock
    private ApolloClientBootstrapArgsMonitorApi bootstrapArgsMonitorApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        configMonitor = new DefaultConfigMonitor();
    }

    @Test
    public void testInit() {
        configMonitor.init(namespaceMonitorApi, threadPoolMonitorApi, exceptionMonitorApi, bootstrapArgsMonitorApi, reporter);

        assertEquals(namespaceMonitorApi, configMonitor.getNamespaceMonitorApi());
        assertEquals(threadPoolMonitorApi, configMonitor.getThreadPoolMonitorApi());
        assertEquals(exceptionMonitorApi, configMonitor.getExceptionMonitorApi());
        assertEquals(bootstrapArgsMonitorApi, configMonitor.getRunningParamsMonitorApi());
    }

    @Test
    public void testGetExporterData() {
        when(reporter.response()).thenReturn("exporter data");

        configMonitor.init(namespaceMonitorApi, threadPoolMonitorApi, exceptionMonitorApi, bootstrapArgsMonitorApi, reporter);
        
        String result = configMonitor.getExporterData();

        assertEquals("exporter data", result);
        verify(reporter).response();
    }

    @Test
    public void testDefaultInstances() {
        assertNotNull(configMonitor.getThreadPoolMonitorApi());
        assertNotNull(configMonitor.getExceptionMonitorApi());
        assertNotNull(configMonitor.getNamespaceMonitorApi());
        assertNotNull(configMonitor.getRunningParamsMonitorApi());
        assertEquals("No Reporter Use", configMonitor.getExporterData()); // Assuming NullApolloClientMetricsExporter returns "null"
    }
}