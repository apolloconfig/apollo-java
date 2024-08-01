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

import com.ctrip.framework.apollo.monitor.api.ApolloClientNamespaceMonitorApi;
import com.ctrip.framework.apollo.monitor.internal.jmx.mbean.ApolloClientJmxNamespaceMBean;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientNamespaceApi.NamespaceMetrics;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Rawven
 */
public class NullClientNamespaceMonitorApi implements ApolloClientNamespaceMonitorApi,
    ApolloClientJmxNamespaceMBean {

  @Override
  public Map<String, NamespaceMetrics> getNamespaceMetrics() {
    return Collections.emptyMap();
  }


  @Override
  public List<String> getNotFoundNamespaces() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getTimeoutNamespaces() {
    return Collections.emptyList();
  }

  @Override
  public Integer getNamespaceItemsNum(String namespace) {
    return 0;
  }

  @Override
  public Integer getConfigFileNum() {
    return 0;
  }

}
