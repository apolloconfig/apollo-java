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
package com.ctrip.framework.apollo.monitor.internal.exporter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.monitor.internal.exporter.impl.DefaultApolloClientMetricsExporterFactory;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

public class DefaultApolloClientMetricsExporterFactoryTest {

    private DefaultApolloClientMetricsExporterFactory factory;

    @Mock
    private ConfigUtil configUtil;

    @Mock
    private ApolloClientMetricsEventListener metricsCollector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MockInjector.setInstance(ConfigUtil.class, configUtil);
        factory = new DefaultApolloClientMetricsExporterFactory();
    }

    @Test
    public void testGetMetricsReporter_NoExternalSystemType() {
        when(configUtil.getMonitorExternalType()).thenReturn(null);

        ApolloClientMetricsExporter result = factory.getMetricsReporter(Collections.emptyList());

        assertNull(result);
        verify(configUtil).getMonitorExternalType();
    }

    @Test
    public void testGetMetricsReporter_ExporterFound() {
        when(configUtil.getMonitorExternalType()).thenReturn("mocktheus");
        when(configUtil.getClientMonitorJmxEnabled()).thenReturn(true);
        when(configUtil.getMonitorExternalExportPeriod()).thenReturn(1000L);
        when(metricsCollector.mBeanName()).thenReturn("testMBean");
        List<ApolloClientMetricsEventListener> collectors = Collections.singletonList(metricsCollector);
        
        ApolloClientMetricsExporter result = factory.getMetricsReporter(collectors);

        assertNotNull(result);
        assertTrue(result instanceof MockApolloClientMetricsExporter);
    }

    @Test
    public void testGetMetricsReporter_ExporterNotFound() {
        when(configUtil.getMonitorExternalType()).thenReturn("unknownType");

        ApolloClientMetricsExporter result = factory.getMetricsReporter(Collections.emptyList());

        assertNull(result);
        verify(configUtil).getMonitorExternalType();
    }

    @Test
    public void testInitializeJmxMonitoring() {
        when(configUtil.getClientMonitorJmxEnabled()).thenReturn(true);
        List<ApolloClientMetricsEventListener> collectors = Collections.singletonList(metricsCollector);

        factory.initializeJmxMonitoring(collectors);
        verify(metricsCollector).mBeanName();
    }
}