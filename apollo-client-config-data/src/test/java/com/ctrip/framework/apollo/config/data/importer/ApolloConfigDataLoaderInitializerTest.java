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
package com.ctrip.framework.apollo.config.data.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.config.data.injector.ApolloConfigDataInjectorCustomizer;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloConfigDataLoaderInitializerTest {

  private final DeferredLogFactory logFactory = destination -> destination.get();

  @Before
  public void setUp() throws Exception {
    resetInitializedFlag();
    clearInjectorCustomizerCaches();
  }

  @After
  public void tearDown() throws Exception {
    resetInitializedFlag();
    clearInjectorCustomizerCaches();
  }

  @Test
  public void testInitApolloClientOnlyOnce() throws Exception {
    Object bootstrapContext = newDefaultBootstrapContext();
    Binder binder = new Binder(new MapConfigurationPropertySource(new LinkedHashMap<>()));
    ApolloConfigDataLoaderInitializer initializer =
        new ApolloConfigDataLoaderInitializer(logFactory, binder, null, bootstrapContext);

    List<PropertySource<?>> firstPropertySources = initializer.initApolloClient();
    List<PropertySource<?>> secondPropertySources = initializer.initApolloClient();

    assertEquals(2, firstPropertySources.size());
    assertTrue(secondPropertySources.isEmpty());
  }

  @Test
  public void testForceDisableBootstrapWhenBootstrapEnabledInConfigDataMode() throws Exception {
    Object bootstrapContext = newDefaultBootstrapContext();
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
    Binder binder = new Binder(new MapConfigurationPropertySource(properties));
    ApolloConfigDataLoaderInitializer initializer =
        new ApolloConfigDataLoaderInitializer(logFactory, binder, null, bootstrapContext);

    List<PropertySource<?>> propertySources = initializer.initApolloClient();

    assertEquals(2, propertySources.size());
    assertTrue(propertySources.get(1) instanceof MapPropertySource);
    MapPropertySource mapPropertySource = (MapPropertySource) propertySources.get(1);
    assertEquals("false",
        mapPropertySource.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED));
    assertEquals("false",
        mapPropertySource.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED));
  }

  private void resetInitializedFlag() throws Exception {
    Field initializedField = ApolloConfigDataLoaderInitializer.class.getDeclaredField("INITIALIZED");
    initializedField.setAccessible(true);
    initializedField.setBoolean(null, false);
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

  private Object newDefaultBootstrapContext() throws Exception {
    String className = "org.springframework.boot.DefaultBootstrapContext";
    if (isClassPresent("org.springframework.boot.bootstrap.DefaultBootstrapContext")) {
      className = "org.springframework.boot.bootstrap.DefaultBootstrapContext";
    }
    Class<?> bootstrapContextClass = Class.forName(className);
    return bootstrapContextClass.getConstructor().newInstance();
  }

  private boolean isClassPresent(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException ex) {
      return false;
    }
  }
}
