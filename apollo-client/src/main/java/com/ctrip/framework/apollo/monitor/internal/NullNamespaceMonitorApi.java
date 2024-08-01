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
package com.ctrip.framework.apollo.monitor.internal;

import com.ctrip.framework.apollo.monitor.api.ApolloNamespaceMonitorApi;
import java.util.Collections;
import java.util.List;

public class NullNamespaceMonitorApi implements ApolloNamespaceMonitorApi {

  @Override
  public String getNamespaceReleaseKey(String namespace) {
    return "";
  }

  @Override
  public long getNamespaceUsageCount(String namespace) {
    return 0;
  }

  @Override
  public String getNamespaceLatestUpdateTime(String namespace) {
    return "";
  }

  @Override
  public long getNamespaceFirstLoadSpend(String namespace) {
    return 0;
  }

  @Override
  public String getNamespace404() {
    return "";
  }

  @Override
  public String getNamespaceTimeout() {
    return "";
  }

  @Override
  public List<String> getNamespaceItemName(String namespace) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllNamespaceReleaseKey() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllNamespaceUsageCount() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllNamespacesLatestUpdateTime() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllUsedNamespaceName() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllNamespaceFirstLoadSpend() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllNamespaceItemName() {
    return Collections.emptyList();
  }
}
