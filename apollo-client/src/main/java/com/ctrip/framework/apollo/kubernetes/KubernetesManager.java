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
package com.ctrip.framework.apollo.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class KubernetesManager {
    private ApiClient client;
    private CoreV1Api coreV1Api;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public KubernetesManager() {
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            coreV1Api = new CoreV1Api(client);
        } catch (Exception e) {
            String errorMessage = "Failed to initialize Kubernetes client: " + e.getMessage();
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public KubernetesManager(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    public V1ConfigMap buildConfigMap(String name, String namespace, Map<String, String> data) {
        V1ObjectMeta metadata = new V1ObjectMeta()
                .name(name)
                .namespace(namespace);

        return new V1ConfigMap()
                .apiVersion("v1")
                .kind("ConfigMap")
                .metadata(metadata)
                .data(data);
    }

    /**
     * Creates a Kubernetes ConfigMap.
     *
     * @param configMapNamespace the namespace of the ConfigMap
     * @param name               the name of the ConfigMap
     * @param data               the data to be stored in the ConfigMap
     * @return the name of the created ConfigMap
     * @throws RuntimeException if an error occurs while creating the ConfigMap
     */
    public String createConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty()) {
            log.error("create configmap failed due to null or empty parameter: configMapNamespace={}, name={}", configMapNamespace, name);
        }
        V1ConfigMap configMap = buildConfigMap(name, configMapNamespace, data);
        try {
            coreV1Api.createNamespacedConfigMap(configMapNamespace, configMap, null, null, null, null);
            log.info("ConfigMap created successfully: name: {}, namespace: {}", name, configMapNamespace);
            return name;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ConfigMap: " + e.getMessage(), e);
        }
    }

    /**
     * get value from config map
     *
     * @param configMapNamespace configMapNamespace
     * @param name               config map name (appId)
     * @param key                config map key (cluster+namespace)
     * @return value(json string)
     */
    public String getValueFromConfigMap(String configMapNamespace, String name, String key) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || key == null || key.isEmpty()) {
            log.error("Parameters can not be null or empty: configMapNamespace={}, name={}", configMapNamespace, name);
            return null;
        }
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            if (!Objects.requireNonNull(configMap.getData()).containsKey(key)) {
                throw new RuntimeException(String.format("Specified key not found in ConfigMap: %s, configMapNamespace: %s, name: %s", name, configMapNamespace, name));
            }
            return configMap.getData().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * update config map
     *
     * @param configMapNamespace configmap namespace
     * @param name               config map name (appId)
     * @param data               new data
     * @return config map name
     */
    // TODO 使用client自带的retry机制，设置重试次数,CAS
    public boolean updateConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || data == null || data.isEmpty()) {
            log.error("Parameters can not be null or empty: configMapNamespace={}, name={}", configMapNamespace, name);
            return false;
        }

        // retry
        int maxRetries = 5;
        int retryCount = 0;
        long waitTime = 100;

        while (retryCount < maxRetries) {
            try {
                V1ConfigMap configmap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
                configmap.setData(data);
                coreV1Api.replaceNamespacedConfigMap(name, configMapNamespace, configmap, null, null, null, null);
                return true;
            } catch (ApiException e) {
                if (e.getCode() == 409) {
                    retryCount++;
                    log.warn("Conflict occurred, retrying... (" + retryCount + ")");
                    try {
                        TimeUnit.MILLISECONDS.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime = Math.min(waitTime * 2, 1000);
                } else {
                    System.err.println("Error updating ConfigMap: " + e.getMessage());
                }
            }
        }
        return retryCount < maxRetries;
    }

    /**
     * check config map exist
     *
     * @param configMapNamespace config map namespace
     * @param configMapName      config map name
     * @return true if config map exist, false otherwise
     */
    public boolean checkConfigMapExist(String configMapNamespace, String configMapName) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || configMapName == null || configMapName.isEmpty()) {
            log.error("Parameters can not be null or empty: configMapNamespace={}, configMapName={}", configMapNamespace, configMapName);
            return false;
        }
        try {
            log.info("Check whether ConfigMap exists, configMapName: {}", configMapName);
            coreV1Api.readNamespacedConfigMap(configMapName, configMapNamespace, null);
            return true;
        } catch (Exception e) {
            // configmap not exist
            return false;
        }
    }
}
