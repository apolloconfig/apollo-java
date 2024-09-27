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
package com.ctrip.framework.apollo.Kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class KubernetesManager {
    private ApiClient client;
    private CoreV1Api coreV1Api;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void initClient() {
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
            log.error("create config map failed due to null or empty parameter: configMapNamespace={}, name={}", configMapNamespace, name);
            throw new IllegalArgumentException("ConfigMap namespace and name cannot be null or empty");
        }
        V1ConfigMap configMap = new V1ConfigMap()
                .metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace))
                .data(data);
        try {
            coreV1Api.createNamespacedConfigMap(configMapNamespace, configMap, null, null, null, null);
            return name;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ConfigMap: " + e.getMessage(), e);
        }
    }

    /**
     * get value from config map
     *
     * @param configMapNamespace
     * @param name               config map name (appId)
     * @return configMap data(all key-value pairs in config map)
     */
    public String loadFromConfigMap(String configMapNamespace, String name) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() ) {
            String errorMessage = String.format("Parameters can not be null or empty, configMapNamespace: '%s', name: '%s'", configMapNamespace, name);
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            log.info("Starting to read ConfigMap: " + name);
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            if (configMap == null) {
                throw new RuntimeException(String.format("ConfigMap does not exist, configMapNamespace: %s, name: %s", configMapNamespace, name));
            }
            Map<String, String> data = configMap.getData();
            if (data != null && data.containsKey(name)) {
                return data.get(name);
            } else {
                throw new RuntimeException(String.format("Specified key not found in ConfigMap: %s, configMapNamespace: %s, name: %s", name, configMapNamespace, name));
            }
        } catch (Exception e) {
            throw new RuntimeException(String
                    .format("get config map failed, configMapNamespace: %s, name: %s", configMapNamespace, name));
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
            if (configMap == null) {
                throw new RuntimeException(String.format("ConfigMap does not exist, configMapNamespace: %s, name: %s", configMapNamespace, name));
            }
            if (!configMap.getData().containsKey(key)) {
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
     * @param configMapNamespace
     * @param name               config map name (appId)
     * @param data               new data
     * @return config map name
     */
    public String updateConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || data == null || data.isEmpty()) {
            log.error("Parameters can not be null or empty: configMapNamespace={}, name={}", configMapNamespace, name);
            return null;
        }
        try {
            V1ConfigMap configMap = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace)).data(data);
            coreV1Api.replaceNamespacedConfigMap(name, configMapNamespace, configMap, null, null, null, "fieldManagerValue");
            return name;
        } catch (Exception e) {
            log.error("update config map failed", e);
            return null;
        }
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
            coreV1Api.readNamespacedConfigMap(configMapName, configMapNamespace, null);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
