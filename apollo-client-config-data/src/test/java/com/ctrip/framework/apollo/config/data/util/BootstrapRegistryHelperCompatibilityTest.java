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
package com.ctrip.framework.apollo.config.data.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.util.ClassUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class BootstrapRegistryHelperCompatibilityTest {

  @Test
  public void testRegisterAndGetFromBootstrapContext() throws Exception {
    Object bootstrapContext = newDefaultBootstrapContext();

    BootstrapRegistryHelper.registerIfAbsent(bootstrapContext, String.class, "apollo");
    BootstrapRegistryHelper.registerIfAbsentFromSupplier(bootstrapContext, Integer.class, () -> 100);

    assertEquals("apollo", BootstrapRegistryHelper.get(bootstrapContext, String.class));
    assertEquals(Integer.valueOf(100), BootstrapRegistryHelper.get(bootstrapContext, Integer.class));
    assertEquals(Boolean.TRUE, BootstrapRegistryHelper.getOrElse(bootstrapContext, Boolean.class,
        Boolean.TRUE));
  }

  @Test
  public void testGetBootstrapContextFromEventAndLoaderContext() throws Exception {
    Object bootstrapContext = newDefaultBootstrapContext();
    ApplicationStartingEvent event =
        newApplicationStartingEvent(bootstrapContext, new SpringApplication(Object.class));

    Object eventBootstrapContext = BootstrapRegistryHelper.getBootstrapContext(event);
    assertSame(bootstrapContext, eventBootstrapContext);

    ConfigDataLoaderContext loaderContext = (ConfigDataLoaderContext) Proxy.newProxyInstance(
        ConfigDataLoaderContext.class.getClassLoader(),
        new Class[]{ConfigDataLoaderContext.class},
        (proxy, method, args) -> {
          if ("getBootstrapContext".equals(method.getName())) {
            return bootstrapContext;
          }
          throw new UnsupportedOperationException("Unexpected method: " + method.getName());
        });
    Object loaderBootstrapContext = BootstrapRegistryHelper.getBootstrapContext(loaderContext);
    assertSame(bootstrapContext, loaderBootstrapContext);
  }

  @Test
  public void testSpringBoot4PresenceDetection() {
    boolean expected = ClassUtils
        .isPresent("org.springframework.boot.bootstrap.ConfigurableBootstrapContext",
            BootstrapRegistryHelperCompatibilityTest.class.getClassLoader());
    assertEquals(expected, BootstrapRegistryHelper.isSpringBoot4Present());
  }

  private ApplicationStartingEvent newApplicationStartingEvent(
      Object bootstrapContext, SpringApplication springApplication) throws Exception {
    for (Constructor<?> constructor : ApplicationStartingEvent.class.getConstructors()) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length == 3 && SpringApplication.class.isAssignableFrom(parameterTypes[1])) {
        return (ApplicationStartingEvent) constructor
            .newInstance(bootstrapContext, springApplication, new String[0]);
      }
    }
    throw new IllegalStateException("Unsupported ApplicationStartingEvent constructor signature");
  }

  private Object newDefaultBootstrapContext() throws Exception {
    String className = "org.springframework.boot.DefaultBootstrapContext";
    if (ClassUtils.isPresent("org.springframework.boot.bootstrap.DefaultBootstrapContext",
        getClass().getClassLoader())) {
      className = "org.springframework.boot.bootstrap.DefaultBootstrapContext";
    }
    return Class.forName(className).getConstructor().newInstance();
  }
}
