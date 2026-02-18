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

import static org.mockito.Mockito.mock;

import com.ctrip.framework.apollo.core.utils.DeferredLogger;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloDeferredLoggerApplicationListenerTest {

  @After
  public void tearDown() {
    DeferredLogger.disable();
  }

  @Test
  public void testReplayDeferredLogsOnApplicationContextInitialized() {
    DeferredLogger.enable();
    ApolloDeferredLoggerApplicationListener listener =
        new ApolloDeferredLoggerApplicationListener();
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    ApplicationContextInitializedEvent event =
        new ApplicationContextInitializedEvent(new SpringApplication(Object.class), new String[0],
            context);

    listener.onApplicationEvent(event);
  }

  @Test
  public void testReplayDeferredLogsOnApplicationFailed() {
    DeferredLogger.enable();
    ApolloDeferredLoggerApplicationListener listener =
        new ApolloDeferredLoggerApplicationListener();
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    ApplicationFailedEvent event =
        new ApplicationFailedEvent(new SpringApplication(Object.class), new String[0], context,
            new IllegalStateException("test"));

    listener.onApplicationEvent(event);
  }
}
