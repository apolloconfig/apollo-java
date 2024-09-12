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
            throw new RuntimeException("k8s client init failed");
        }
    }

    public String createConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        if (configMapNamespace == null || configMapNamespace == "" || name == null || name == "") {
            log.error("create config map failed due to null parameter");
            return null;
        }
        V1ConfigMap configMap = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace)).data(data);
        try {
            coreV1Api.createNamespacedConfigMap(configMapNamespace, configMap, null, null, null,null);
            return name;
        } catch (Exception e) {
            log.error("create config map failed", e);
            return null;
        }
    }

    public String loadFromConfigMap(String configMapNamespace, String name) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || name == null || name.isEmpty()) {
            log.error("参数不能为空");
            return null;
        }
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            if (configMap == null) {
                log.error("ConfigMap不存在");
                return null;
            }
            Map<String, String> data = configMap.getData();
            if (data != null && data.containsKey(name)) {
                return data.get(name);
            } else {
                log.error("在ConfigMap中未找到指定的键: " + name);
                return null;
            }
        } catch (Exception e) {
            log.error("get config map failed", e);
            return null;
        }
    }

    public String getValueFromConfigMap(String configMapNamespace, String name, String key) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || key == null || key.isEmpty()) {
            log.error("参数不能为空");
            return null;
        }
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            if (configMap == null || configMap.getData() == null) {
                log.error("ConfigMap不存在或没有数据");
                return null;
            }
            if (!configMap.getData().containsKey(key)) {
                log.error("在ConfigMap中未找到指定的键: " + key);
                return null;
            }
            return configMap.getData().get(key);
        } catch (Exception e) {
            log.error("get config map failed", e);
            return null;
        }
    }

    public String updateConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || name == null || name.isEmpty() || data == null || data.isEmpty()) {
            log.error("参数不能为空");
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

    public boolean checkConfigMapExist(String configMapNamespace, String configMapName) {
        if (configMapNamespace == null || configMapNamespace.isEmpty() || configMapName == null || configMapName.isEmpty()) {
            log.error("参数不能为空");
            return false;
        }
        try {
            coreV1Api.readNamespacedConfigMap(configMapName, configMapNamespace, null);
            return true;
        } catch (Exception e) {
            log.error("check config map failed", e);
            return false;
        }
    }
}
