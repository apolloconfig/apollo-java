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
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMetricsEventListenerManager;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * @author Rawven
 */
public class ApolloConfigMetricsEventPublisher {

  private static final ApolloClientMetricsEventListenerManager COLLECTOR_MANAGER = ApolloInjector.getInstance(
      ApolloClientMetricsEventListenerManager.class);
  private static final ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  public static void publish(ApolloConfigMetricsEvent event) {
    if (m_configUtil.getClientMonitorEnabled()) {
      for (ApolloClientMetricsEventListener collector : COLLECTOR_MANAGER.getCollectors()) {
        if (collector.isSupport(event)) {
          collector.collect(event);
          return;
        }
      }
    }
  }
}


