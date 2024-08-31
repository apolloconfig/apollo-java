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
package com.ctrip.framework.apollo.monitor.internal.event;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorContext;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * @author Rawven
 */
public class ApolloClientMonitorEventPublisher {

  private static ApolloClientMonitorContext COLLECTOR_MANAGER = ApolloInjector.getInstance(
      ApolloClientMonitorContext.class);
  private static ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  public static void publish(ApolloClientMonitorEvent event) {
    if (m_configUtil.isClientMonitorEnabled()) {
      for (ApolloClientMonitorEventListener collector : COLLECTOR_MANAGER.getCollectors()) {
        if (collector.isSupported(event)) {
          collector.collect(event);
          return;
        }
      }
    }
  }

  protected static void reset() {
    COLLECTOR_MANAGER = ApolloInjector.getInstance(
        ApolloClientMonitorContext.class);
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  }
}


