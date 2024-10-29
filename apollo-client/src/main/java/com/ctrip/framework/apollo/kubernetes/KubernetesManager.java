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

import com.ctrip.framework.apollo.core.utils.StringUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public KubernetesManager() {
        try {
            client = Config.defaultClient();
            coreV1Api = new CoreV1Api(client);
        } catch (Exception e) {
            String errorMessage = "Failed to initialize Kubernetes client: " + e.getMessage();
            logger.error(errorMessage, e);
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
     * @param k8sNamespace the namespace of the ConfigMap
     * @param name               the name of the ConfigMap
     * @param data               the data to be stored in the ConfigMap
     * @return the name of the created ConfigMap
     * @throws RuntimeException if an error occurs while creating the ConfigMap
     */
    public String createConfigMap(String k8sNamespace, String name, Map<String, String> data) {
        if (StringUtils.isEmpty(k8sNamespace) || StringUtils.isEmpty(name)) {
            logger.error("create configmap failed due to null or empty parameter: k8sNamespace={}, name={}", k8sNamespace, name);
            return null;
        }
        V1ConfigMap configMap = buildConfigMap(name, k8sNamespace, data);
        try {
            coreV1Api.createNamespacedConfigMap(k8sNamespace, configMap, null, null, null, null);
            logger.info("ConfigMap created successfully: name: {}, namespace: {}", name, k8sNamespace);
            return name;
        } catch (Exception e) {
            logger.error("Failed to create ConfigMap: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create ConfigMap: " + e.getMessage(), e);
        }
    }

    /**
     * get value from config map
     *
     * @param k8sNamespace k8sNamespace
     * @param name               config map name
     * @param key                config map key (cluster+namespace)
     * @return value(json string)
     */
    public String getValueFromConfigMap(String k8sNamespace, String name, String key) {
        if (StringUtils.isEmpty(k8sNamespace) || StringUtils.isEmpty(name) || StringUtils.isEmpty(key)) {
            logger.error("Parameters can not be null or empty: k8sNamespace={}, name={}", k8sNamespace, name);
            return null;
        }
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, k8sNamespace, null);
            if (!Objects.requireNonNull(configMap.getData()).containsKey(key)) {
                logger.error("Specified key not found in ConfigMap: {}, k8sNamespace: {}, name: {}", name, k8sNamespace, name);
            }
            return configMap.getData().get(key);
        } catch (Exception e) {
            logger.error("Error occurred while getting value from ConfigMap: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * update config map
     *
     * @param k8sNamespace configmap namespace
     * @param name               config map name
     * @param data               new data
     * @return config map name
     */
    // Set the retry times using the client retry mechanism (CAS)
    public boolean updateConfigMap(String k8sNamespace, String name, Map<String, String> data) {
        if (StringUtils.isEmpty(k8sNamespace) || StringUtils.isEmpty(name)) {
            logger.error("Parameters can not be null or empty: k8sNamespace={}, name={}", k8sNamespace, name);
            return false;
        }

        // retry
        int maxRetries = 5;
        int retryCount = 0;
        long waitTime = 100;

        while (retryCount < maxRetries) {
            try {
                V1ConfigMap configmap = coreV1Api.readNamespacedConfigMap(name, k8sNamespace, null);
                configmap.setData(data);
                coreV1Api.replaceNamespacedConfigMap(name, k8sNamespace, configmap, null, null, null, null);
                return true;
            } catch (ApiException e) {
                if (e.getCode() == 409) {
                    retryCount++;
                    logger.warn("Conflict occurred, retrying... ({})", retryCount);
                    try {
                        TimeUnit.MILLISECONDS.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime = Math.min(waitTime * 2, 1000);
                } else {
                    logger.error("Error updating ConfigMap: {}", e.getMessage(), e);
                }
            }
        }
        return false;
    }

    /**
     * check config map exist
     *
     * @param k8sNamespace config map namespace
     * @param configMapName      config map name
     * @return true if config map exist, false otherwise
     */
    public boolean checkConfigMapExist(String k8sNamespace, String configMapName) {
        if (StringUtils.isEmpty(k8sNamespace) || StringUtils.isEmpty(configMapName)) {
            logger.error("Parameters can not be null or empty: k8sNamespace={}, configMapName={}", k8sNamespace, configMapName);
            return false;
        }
        try {
            logger.info("Check whether ConfigMap exists, configMapName: {}", configMapName);
            coreV1Api.readNamespacedConfigMap(configMapName, k8sNamespace, null);
            return true;
        } catch (Exception e) {
            // configmap not exist
            logger.error("Error checking ConfigMap existence: {}", e.getMessage(), e);
            return false;
        }
    }
}
