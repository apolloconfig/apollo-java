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
package com.ctrip.framework.apollo.config.data.extension.initialize;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.config.data.injector.ApolloConfigDataInjectorCustomizer;
import com.ctrip.framework.apollo.util.http.HttpClient;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientExtensionInitializeFactoryTest {

  private final DeferredLogFactory logFactory = destination -> destination.get();

  @Before
  public void setUp() throws Exception {
    clearInjectorCustomizerCaches();
  }

  @After
  public void tearDown() throws Exception {
    clearInjectorCustomizerCaches();
  }

  @Test
  public void testInitializeExtensionDisabledByDefault() {
    ApolloClientExtensionInitializeFactory factory =
        new ApolloClientExtensionInitializeFactory(logFactory, new Object());
    Binder binder = new Binder(new MapConfigurationPropertySource(new LinkedHashMap<>()));

    factory.initializeExtension(binder, null);

    assertFalse(ApolloConfigDataInjectorCustomizer.isRegistered(HttpClient.class));
  }

  @Test
  public void testInitializeLongPollingExtension() {
    ApolloClientExtensionInitializeFactory factory =
        new ApolloClientExtensionInitializeFactory(logFactory, new Object());
    Map<String, String> map = new LinkedHashMap<>();
    map.put("apollo.client.extension.enabled", "true");
    map.put("apollo.client.extension.messaging-type", "long_polling");
    Binder binder = new Binder(new MapConfigurationPropertySource(map));

    factory.initializeExtension(binder, null);

    assertTrue(ApolloConfigDataInjectorCustomizer.isRegistered(HttpClient.class));
  }

  @Test
  public void testInitializeWebsocketExtensionThrowsException() {
    ApolloClientExtensionInitializeFactory factory =
        new ApolloClientExtensionInitializeFactory(logFactory, new Object());
    Map<String, String> map = new LinkedHashMap<>();
    map.put("apollo.client.extension.enabled", "true");
    map.put("apollo.client.extension.messaging-type", "websocket");
    Binder binder = new Binder(new MapConfigurationPropertySource(map));

    try {
      factory.initializeExtension(binder, null);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException ex) {
      assertTrue(ex.getMessage().contains("websocket support is not complete yet"));
    }
  }

  private void clearInjectorCustomizerCaches() throws Exception {
    Field instanceSuppliers =
        ApolloConfigDataInjectorCustomizer.class.getDeclaredField("INSTANCE_SUPPLIERS");
    instanceSuppliers.setAccessible(true);
    ((Map<?, ?>) instanceSuppliers.get(null)).clear();

    Field instances = ApolloConfigDataInjectorCustomizer.class.getDeclaredField("INSTANCES");
    instances.setAccessible(true);
    ((Map<?, ?>) instances.get(null)).clear();
  }
}
