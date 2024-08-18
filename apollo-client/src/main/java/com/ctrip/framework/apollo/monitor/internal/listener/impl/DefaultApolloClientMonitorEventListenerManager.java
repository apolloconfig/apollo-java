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
package com.ctrip.framework.apollo.monitor.internal.listener.impl;

import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListener;
import com.ctrip.framework.apollo.monitor.internal.listener.ApolloClientMonitorEventListenerManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rawven
 */
public class DefaultApolloClientMonitorEventListenerManager implements
    ApolloClientMonitorEventListenerManager {

  private List<ApolloClientMonitorEventListener> collectors = new ArrayList<>();

  public DefaultApolloClientMonitorEventListenerManager() {
  }

  @Override
  public List<ApolloClientMonitorEventListener> getCollectors() {
    return collectors;
  }

  public void setCollectors(List<ApolloClientMonitorEventListener> collectors) {
    this.collectors.clear();
    this.collectors.addAll(collectors);
  }

}
