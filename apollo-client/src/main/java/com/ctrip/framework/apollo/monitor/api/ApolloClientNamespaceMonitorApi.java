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
package com.ctrip.framework.apollo.monitor.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Rawven
 */
public interface ApolloClientNamespaceMonitorApi {

  /**
   * NamespaceMetrics: 1.usageCount 2.firstLoadSpend 3.latestUpdateTime 4.releaseKey
   */
  Map<String, NamespaceMetrics> getNamespaceMetrics();

  /**
   * get Namespace Config.ItemsNum
   */
  Integer getNamespacePropertySize(String namespace);

  /**
   * get ConfigFile namespaces
   */
  List<String> getConfigFileNamespaces();

  /**
   * get not found namespaces
   */
  List<String> getNotFoundNamespaces();

  /**
   * get timeout namespaces
   */
  List<String> getTimeoutNamespaces();


  class NamespaceMetrics {

    private int usageCount;
    private long firstLoadTimeSpendInMs;
    private LocalDateTime latestUpdateTime = LocalDateTime.now();
    private String releaseKey = "";

    public String getReleaseKey() {
      return releaseKey;
    }

    public void setReleaseKey(String releaseKey) {
      this.releaseKey = releaseKey;
    }

    public int getUsageCount() {
      return usageCount;
    }

    public void incrementUsageCount() {
      usageCount++;
    }

    public long getFirstLoadTimeSpendInMs() {
      return firstLoadTimeSpendInMs;
    }

    public void setFirstLoadTimeSpendInMs(long firstLoadTimeSpendInMs) {
      this.firstLoadTimeSpendInMs = firstLoadTimeSpendInMs;
    }

    public LocalDateTime getLatestUpdateTime() {
      return latestUpdateTime;
    }

    public void setLatestUpdateTime(LocalDateTime latestUpdateTime) {
      this.latestUpdateTime = latestUpdateTime;
    }
  }


}
