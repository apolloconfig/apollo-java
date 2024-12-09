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

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class KubernetesManager {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesManager.class);

    private ApiClient client;
    private CoreV1Api coreV1Api;
    private int propertyKubernetesMaxWritePods;
    private String localPodName = System.getenv("HOSTNAME");

    public KubernetesManager() {
        try {
            client = Config.defaultClient();
            coreV1Api = new CoreV1Api(client);
            ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
            propertyKubernetesMaxWritePods = configUtil.getPropertyKubernetesMaxWritePods();
        } catch (Exception e) {
            String errorMessage = "Failed to initialize Kubernetes client: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @VisibleForTesting
    public KubernetesManager(CoreV1Api coreV1Api, String localPodName, int propertyKubernetesMaxWritePods) {
        this.coreV1Api = coreV1Api;
        this.localPodName = localPodName;
        this.propertyKubernetesMaxWritePods = propertyKubernetesMaxWritePods;
    }

    private V1ConfigMap buildConfigMap(String name, String namespace, Map<String, String> data) {
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
    public boolean updateConfigMap(String k8sNamespace, String name, Map<String, String> data) throws ApiException {
        if (StringUtils.isEmpty(k8sNamespace) || StringUtils.isEmpty(name)) {
            logger.error("Parameters can not be null or empty: k8sNamespace={}, name={}", k8sNamespace, name);
            return false;
        }

        if (!isWritePod(k8sNamespace)) {
            return true;
        }

        int maxRetries = 5;
        int retryCount = 0;
        long waitTime = 100;

        while (retryCount < maxRetries) {
            try {
                V1ConfigMap configmap = coreV1Api.readNamespacedConfigMap(name, k8sNamespace, null);
                Map<String, String> existingData = configmap.getData();
                if (existingData == null) {
                    existingData = new HashMap<>();
                }

                // Determine if the data contains its own kv and de-weight it
                Map<String, String> finalExistingData = existingData;
                boolean containsEntry = data.entrySet().stream()
                        .allMatch(entry -> entry.getValue().equals(finalExistingData.get(entry.getKey())));

                if (containsEntry) {
                    logger.info("Data is identical or already contains the entry, no update needed.");
                    return true;
                }

                // Add new entries to the existing data
                existingData.putAll(data);
                configmap.setData(existingData);

                coreV1Api.replaceNamespacedConfigMap(name, k8sNamespace, configmap, null, null, null, null);
                return true;
            } catch (ApiException e) {
                if (e.getCode() == 409) {
                    retryCount++;
                    logger.warn("Conflict occurred, retrying... ({})", retryCount);
                    try {
                        // Scramble the time, so that different machines in the distributed retry time is different
                        // The random ratio ranges from 0.9 to 1.1
                        TimeUnit.MILLISECONDS.sleep((long) (waitTime * (0.9 + Math.random() * 0.2)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime = Math.min(waitTime * 2, 1000);
                } else {
                    logger.error("Error updating ConfigMap: {}", e.getMessage(), e);
                    throw e;
                }
            }
        }
        String errorMessage = String.format("Failed to update ConfigMap after %d retries: k8sNamespace=%s, name=%s", maxRetries, k8sNamespace, name);
        logger.error(errorMessage);
        throw new ApolloConfigException(errorMessage);
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
            logger.info("ConfigMap not existence");
            return false;
        }
    }

    /**
     * check pod whether pod can write configmap
     *
     * @param k8sNamespace config map namespace
     * @return true if this pod can write configmap, false otherwise
     */
    private boolean isWritePod(String k8sNamespace) {
        try {
            if (Strings.isNullOrEmpty(localPodName)) {
                return true;
            }
            V1Pod localPod = coreV1Api.readNamespacedPod(localPodName, k8sNamespace, null);
            V1ObjectMeta localMetadata = localPod.getMetadata();
            if (localMetadata == null || localMetadata.getLabels() == null) {
                return true;
            }
            String appName = localMetadata.getLabels().get("app");
            String labelSelector = "app=" + appName;

            V1PodList v1PodList = coreV1Api.listNamespacedPod(k8sNamespace, null, null,
                    null, null, labelSelector,
                    null, null, null
                    , null, null);

            return v1PodList.getItems().stream()
                    .map(V1Pod::getMetadata)
                    .filter(Objects::nonNull)
                    //Make each node selects the same write nodes by sorting
                    .filter(metadata -> metadata.getCreationTimestamp() != null)
                    .sorted(Comparator.comparing(V1ObjectMeta::getCreationTimestamp))
                    .map(V1ObjectMeta::getName)
                    .limit(propertyKubernetesMaxWritePods)
                    .anyMatch(localPodName::equals);
        } catch (Exception e) {
            logger.info("Error determining write pod eligibility:{}", e.getMessage(), e);
            return true;
        }
    }
}
