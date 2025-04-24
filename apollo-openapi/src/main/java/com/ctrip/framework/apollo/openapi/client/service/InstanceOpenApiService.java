/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.client.service;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.openapi.client.url.OpenApiPathBuilder;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class InstanceOpenApiService extends AbstractOpenApiService implements
        com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService {

    public InstanceOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
        super(client, baseUrl, gson);
    }

    @Override
    public int getInstanceCountByNamespace(String appId, String env, String clusterName, String namespaceName) {
        if (Strings.isNullOrEmpty(clusterName)) {
            clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
        }
        if (Strings.isNullOrEmpty(namespaceName)) {
            namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
        }

        checkNotEmpty(appId, "App id");
        checkNotEmpty(env, "Env");

        OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
                .envsPathVal(env)
                .appsPathVal(appId)
                .clustersPathVal(clusterName)
                .namespacesPathVal(namespaceName)
                .customResource("instances");

        try (CloseableHttpResponse response = get(pathBuilder)) {
            return gson.fromJson(EntityUtils.toString(response.getEntity()), Integer.class);
        } catch (Throwable ex) {
            throw new RuntimeException(String.format("Get instance count: appId: %s, cluster: %s, namespace: %s in env: %s failed",
                    appId, clusterName, namespaceName, env), ex);
        }
    }
}