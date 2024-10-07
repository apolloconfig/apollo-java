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
package com.ctrip.framework.apollo.openapi.api;

import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import java.util.List;

/**
 * @author wxq
 */
public interface NamespaceOpenApiService {

  OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName, boolean fillItemDetail);

  List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName, boolean fillItemDetail);

  OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO);

  OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
      String namespaceName);
}
