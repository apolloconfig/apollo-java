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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.monitor.api.ConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.DefaultConfigMonitor;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListenerManager;
import com.ctrip.framework.apollo.monitor.internal.listener.DefaultApolloClientMonitorEventListenerManager;
import com.ctrip.framework.apollo.monitor.internal.tracer.ApolloClientMessageProducerComposite;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class ConfigMonitorInitializerTest {

    private ConfigUtil mockConfigUtil;
    private Logger mockLogger;
    private DefaultApolloClientMonitorEventListenerManager mockManager;
    private ApolloClientMetricsExporter mockMetricsExporter;
    private DefaultConfigManager mockConfigManager;
    private DefaultConfigMonitor mockConfigMonitor;

    @Before
    public void setUp() {
        mockConfigUtil = mock(ConfigUtil.class);
        when(mockConfigUtil.getMonitorExceptionQueueSize()).thenReturn(100);
        when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(false);
        mockLogger = mock(Logger.class);
        mockManager = mock(DefaultApolloClientMonitorEventListenerManager.class);
        mockMetricsExporter = mock(ApolloClientMetricsExporter.class);
        mockConfigManager = mock(DefaultConfigManager.class);
        mockConfigMonitor = mock(DefaultConfigMonitor.class);
        

        // Mock static methods
        MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);
        MockInjector.setInstance(ApolloClientMonitorEventListenerManager.class, mockManager);
        MockInjector.setInstance(ConfigManager.class, mockConfigManager);
        MockInjector.setInstance(ConfigMonitor.class, mockConfigMonitor);
        
        // Reset static state before each test
        ConfigMonitorInitializer.reset();
    }

    @Test
    public void testInitialize_WhenEnabledAndNotInitialized() {
        when(mockManager.getCollectors()).thenReturn(Collections.emptyList());
        doReturn(true).when(mockConfigUtil).getClientMonitorEnabled();
        ConfigMonitorInitializer.initialize();

        verify(mockManager).setCollectors(anyList());
        verify(mockConfigMonitor).init(any(), any(), any(), any(), any());
        assertTrue(ConfigMonitorInitializer.hasInitialized); // Check hasInitialized flag
    }

    @Test
    public void testInitialize_WhenAlreadyInitialized() {
        ConfigMonitorInitializer.hasInitialized = true;

        ConfigMonitorInitializer.initialize();

        verify(mockConfigMonitor, never()).init(any(), any(), any(), any(), any());
    }

    @Test
    public void testInitialize_WhenClientMonitorDisabled() {
        when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(false);

        ConfigMonitorInitializer.initialize();

        verify(mockManager, never()).setCollectors(anyList());
        verify(mockConfigMonitor, never()).init(any(), any(), any(), any(), any());
    }

    @Test
    public void testInitializeMessageProducerComposite() {
        when(mockConfigUtil.getClientMonitorEnabled()).thenReturn(true);

        ApolloClientMessageProducerComposite composite = ConfigMonitorInitializer.initializeMessageProducerComposite();

        assertNotNull(composite);
        // Additional assertions can be added based on expected behavior
    }
        @Test
    public void testInitializeJmxMonitoring() {
        when(mockConfigUtil.getClientMonitorJmxEnabled()).thenReturn(true);
        ApolloClientMonitorEventListener metricsCollector = mock(ApolloClientMonitorEventListener.class);
        List<ApolloClientMonitorEventListener> collectors = Collections.singletonList(metricsCollector);

        ConfigMonitorInitializer.initializeJmxMonitoring(collectors);
        verify(metricsCollector).mBeanName();
    }
    
}