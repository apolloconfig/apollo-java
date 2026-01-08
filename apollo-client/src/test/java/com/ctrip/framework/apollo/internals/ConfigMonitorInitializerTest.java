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

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorContext;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporterFactory;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigMonitorInitializerTest {

	@Mock
	private ConfigUtil mockConfigUtil;
	@Mock
	private ApolloClientMonitorContext mockMonitorContext;
	@Mock
	private ApolloClientMetricsExporterFactory mockExporterFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);
		MockInjector.setInstance(ApolloClientMonitorContext.class, mockMonitorContext);
		MockInjector.setInstance(ApolloClientMetricsExporterFactory.class, mockExporterFactory);
		resetConfigMonitorInitializer();
	}

	@AfterEach
	public void tearDown() throws Exception {
		MockInjector.reset();
	}

	@Test
	public void testInitializeWhenMonitorEnabledAndNotInitialized() {
		when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(true);
		ConfigMonitorInitializer.initialize();
		assertTrue(ConfigMonitorInitializer.hasInitialized);
		//ConfigMonitorInitializer.53line + DefaultApolloClientBootstrapArgsApi.64line
		verify(mockConfigUtil, times(2)).isClientMonitorEnabled();
	}

	@Test
	public void testInitializeWhenMonitorDisabled() {
		when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(false);
		ConfigMonitorInitializer.initialize();
		assertFalse(ConfigMonitorInitializer.hasInitialized);
	}

	@Test
	public void testInitializeWhenAlreadyInitialized() {
		when(mockConfigUtil.isClientMonitorEnabled()).thenReturn(true);
		ConfigMonitorInitializer.hasInitialized = true;
		ConfigMonitorInitializer.initialize();
		verify(mockConfigUtil, times(1)).isClientMonitorEnabled();
	}

	@Test
	public void testReset() {
		ConfigMonitorInitializer.reset();
		assertFalse(ConfigMonitorInitializer.hasInitialized);
	}

	private void resetConfigMonitorInitializer() {
		ConfigMonitorInitializer.reset();
	}

}
