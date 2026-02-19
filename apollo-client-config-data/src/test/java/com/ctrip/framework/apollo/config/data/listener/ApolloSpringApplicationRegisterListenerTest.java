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
package com.ctrip.framework.apollo.config.data.listener;

import static org.junit.Assert.assertSame;

import com.ctrip.framework.apollo.config.data.util.BootstrapRegistryHelper;
import java.lang.reflect.Constructor;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.util.ClassUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloSpringApplicationRegisterListenerTest {

  @Test
  public void testRegisterSpringApplicationToBootstrapContext() throws Exception {
    Object bootstrapContext = newDefaultBootstrapContext();
    SpringApplication springApplication = new SpringApplication(Object.class);

    ApplicationStartingEvent event = newApplicationStartingEvent(bootstrapContext, springApplication);

    ApolloSpringApplicationRegisterListener listener = new ApolloSpringApplicationRegisterListener();
    listener.onApplicationEvent(event);

    SpringApplication registered = BootstrapRegistryHelper.get(bootstrapContext, SpringApplication.class);
    assertSame(springApplication, registered);
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
