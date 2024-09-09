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
        V1ConfigMap configMap = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace)).data(data);
        try {
            coreV1Api.createNamespacedConfigMap(configMapNamespace, configMap, null, null, null,null);
            return name;
        } catch (Exception e) {
            log.error("create config map failed", e);
            return null;
        }
    }

    public Map<String, String> getFromConfigMap(String configMapNamespace, String name) {
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            return configMap.getData();
        } catch (Exception e) {
            log.error("get config map failed", e);
            return null;
        }
    }

    public Map<String, Object> loadFromConfigMap(String configMapNamespace, String name, String key) {
        try {
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(name, configMapNamespace, null);
            String jsonStr = configMap.getData().get(key);

        } catch (Exception e) {
            log.error("get config map failed", e);
            return null;
        }
    }

    public String updateConfigMap(String configMapNamespace, String name, Map<String, String> data) {
        try {
            V1ConfigMap configMap = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(configMapNamespace)).data(data);
            coreV1Api.replaceNamespacedConfigMap(name, configMapNamespace, configMap, null, null, null, null);
            return name;
        } catch (Exception e) {
            log.error("update config map failed", e);
            return null;
        }
    }

}
