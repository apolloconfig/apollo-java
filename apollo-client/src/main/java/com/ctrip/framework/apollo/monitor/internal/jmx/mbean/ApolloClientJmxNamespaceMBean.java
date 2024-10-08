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
package com.ctrip.framework.apollo.monitor.internal.jmx.mbean;

import java.util.List;
import java.util.Map;
import javax.management.MXBean;

/**
 * @author Rawven
 */
@MXBean
public interface ApolloClientJmxNamespaceMBean {
  // Because JMX does not support all type return values
  // declare the interface separately.

  /**
   * NamespaceMetrics: 1.usageCount 2.firstLoadSpend 3.latestUpdateTime 4.releaseKey
   */
  Map<String, NamespaceMetricsString> getNamespaceMetricsString();

  /**
   * get Namespace Config.ItemsNum
   */
  Integer getNamespacePropertySize(String namespace);

  /**
   * get not found namespaces
   */
  List<String> getNotFoundNamespaces();

  /**
   * get timeout namespaces
   */
  List<String> getTimeoutNamespaces();


  class NamespaceMetricsString {

    private int usageCount;
    private long firstLoadTimeSpendInMs;
    private String latestUpdateTime;
    private String releaseKey = "";

    public int getUsageCount() {
      return usageCount;
    }

    public void setUsageCount(int usageCount) {
      this.usageCount = usageCount;
    }

    public long getFirstLoadTimeSpendInMs() {
      return firstLoadTimeSpendInMs;
    }

    public void setFirstLoadTimeSpendInMs(long firstLoadTimeSpendInMs) {
      this.firstLoadTimeSpendInMs = firstLoadTimeSpendInMs;
    }

    public String getLatestUpdateTime() {
      return latestUpdateTime;
    }

    public void setLatestUpdateTime(String latestUpdateTime) {
      this.latestUpdateTime = latestUpdateTime;
    }

    public String getReleaseKey() {
      return releaseKey;
    }

    public void setReleaseKey(String releaseKey) {
      this.releaseKey = releaseKey;
    }
  }

}
