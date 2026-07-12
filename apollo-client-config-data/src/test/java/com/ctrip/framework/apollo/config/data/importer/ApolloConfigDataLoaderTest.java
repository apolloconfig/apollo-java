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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.config.data.injector.ApolloMockInjectorCustomizer;
import com.ctrip.framework.apollo.config.data.util.BootstrapRegistryHelper;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.google.common.collect.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloConfigDataLoaderTest {

  private final DeferredLogFactory logFactory = destination -> destination.get();

  @Before
  public void setUp() throws Exception {
    clearApolloClientCaches();
    resetInitializer();
    resetConfigService();
    ApolloMockInjectorCustomizer.clear();
    System.setProperty("app.id", "loader-test-app");
    System.setProperty("env", "local");
  }

  @After
  public void tearDown() throws Exception {
    ApolloMockInjectorCustomizer.clear();
    resetInitializer();
    resetConfigService();
    clearApolloClientCaches();
    System.clearProperty("app.id");
    System.clearProperty("env");
  }

  @Test
  public void testLoadPropertySourceOrderAndInitializerReuse() throws Exception {
    ApolloMockInjectorCustomizer.register(ConfigFactory.class, this::newConfigFactory);

    Object bootstrapContext = newDefaultBootstrapContext();
    Binder binder = new Binder(new MapConfigurationPropertySource(new LinkedHashMap<>()));
    BootstrapRegistryHelper.registerIfAbsent(bootstrapContext, Binder.class, binder);
    ConfigDataLoaderContext context = newContextWithBootstrapContext(bootstrapContext);

    ApolloConfigDataLoader loader = new ApolloConfigDataLoader(logFactory);

    ConfigData firstConfigData = loader.load(context, new ApolloConfigDataResource("application"));
    assertEquals(3, firstConfigData.getPropertySources().size());
    assertEquals("application", firstConfigData.getPropertySources().get(0).getName());
    assertEquals(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME,
        firstConfigData.getPropertySources().get(1).getName());
    assertEquals(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME,
        firstConfigData.getPropertySources().get(2).getName());

    ConfigData secondConfigData = loader.load(context, new ApolloConfigDataResource("TEST1.apollo"));
    assertEquals(1, secondConfigData.getPropertySources().size());
    PropertySource<?> secondPropertySource = secondConfigData.getPropertySources().get(0);
    assertEquals("TEST1.apollo", secondPropertySource.getName());
    assertEquals("v2", secondPropertySource.getProperty("key2"));
  }

  private ConfigFactory newConfigFactory() {
    Map<String, Config> configMap = new HashMap<>();
    configMap.put("application", mockConfig(singletonConfig("key1", "v1")));
    configMap.put("TEST1.apollo", mockConfig(singletonConfig("key2", "v2")));
    return new ConfigFactory() {
      @Override
      public Config create(String namespace) {
        return create("loader-test-app", namespace);
      }

      @Override
      public Config create(String appId, String namespace) {
        Config config = configMap.get(namespace);
        return config != null ? config : mockConfig(new HashMap<>());
      }

      @Override
      public com.ctrip.framework.apollo.ConfigFile createConfigFile(String namespace,
          com.ctrip.framework.apollo.core.enums.ConfigFileFormat configFileFormat) {
        return null;
      }

      @Override
      public com.ctrip.framework.apollo.ConfigFile createConfigFile(String appId, String namespace,
          com.ctrip.framework.apollo.core.enums.ConfigFileFormat configFileFormat) {
        return null;
      }
    };
  }

  private Config mockConfig(Map<String, String> properties) {
    Config config = mock(Config.class);
    Set<String> propertyNames = properties.keySet();
    when(config.getPropertyNames()).thenReturn(propertyNames);
    when(config.getProperty(anyString(), nullable(String.class))).thenAnswer(invocation -> {
      String key = invocation.getArgument(0, String.class);
      String defaultValue = invocation.getArgument(1);
      return properties.getOrDefault(key, defaultValue);
    });
    return config;
  }

  private Map<String, String> singletonConfig(String key, String value) {
    Map<String, String> map = new HashMap<>();
    map.put(key, value);
    return map;
  }

  private ConfigDataLoaderContext newContextWithBootstrapContext(Object bootstrapContext) {
    return (ConfigDataLoaderContext) Proxy.newProxyInstance(
        ConfigDataLoaderContext.class.getClassLoader(),
        new Class[]{ConfigDataLoaderContext.class},
        (proxy, method, args) -> {
          if ("getBootstrapContext".equals(method.getName())) {
            return bootstrapContext;
          }
          throw new UnsupportedOperationException("Unexpected method: " + method.getName());
        });
  }

  private void resetInitializer() throws Exception {
    Field initializedField = ApolloConfigDataLoaderInitializer.class.getDeclaredField("INITIALIZED");
    initializedField.setAccessible(true);
    initializedField.setBoolean(null, false);
  }

  private void resetConfigService() throws Exception {
    Method resetMethod = ConfigService.class.getDeclaredMethod("reset");
    resetMethod.setAccessible(true);
    resetMethod.invoke(null);
  }

  private void clearApolloClientCaches() throws Exception {
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configs");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configLocks");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFiles");
    clearField(ApolloInjector.getInstance(ConfigManager.class), "m_configFileLocks");
    clearField(ApolloInjector.getInstance(ConfigFactoryManager.class), "m_factories");
    clearField(ApolloInjector.getInstance(ConfigRegistry.class), "m_instances");
  }

  @SuppressWarnings("unchecked")
  private void clearField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object container = field.get(target);
    if (container instanceof Map) {
      ((Map<?, ?>) container).clear();
      return;
    }
    if (container instanceof Table) {
      ((Table<?, ?, ?>) container).clear();
      return;
    }
    Method clearMethod = container.getClass().getDeclaredMethod("clear");
    clearMethod.setAccessible(true);
    clearMethod.invoke(container);
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
