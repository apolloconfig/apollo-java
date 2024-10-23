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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.kubernetes.KubernetesManager;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author dyx1234
 */
public class K8sConfigMapConfigRepository extends AbstractConfigRepository
        implements RepositoryChangeListener {
    private static final Logger logger = DeferredLoggerFactory.getLogger(K8sConfigMapConfigRepository.class);
    private final String namespace;
    private String configMapName;
    private String configMapKey;
    private final String k8sNamespace;
    private final ConfigUtil configUtil;
    private final KubernetesManager kubernetesManager;
    private volatile Properties configMapProperties;
    private volatile ConfigRepository upstream;
    private volatile ConfigSourceType sourceType = ConfigSourceType.CONFIGMAP;

    /**
     * Constructor
     *
     * @param namespace the namespace
     */
    public K8sConfigMapConfigRepository(String namespace) {
        this(namespace, (ConfigRepository) null);
    }

    public K8sConfigMapConfigRepository(String namespace, KubernetesManager kubernetesManager) {
        this.namespace = namespace;
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        k8sNamespace = configUtil.getK8sNamespace();
        this.kubernetesManager = kubernetesManager;

        this.setConfigMapKey(configUtil.getCluster(), namespace);
        this.setConfigMapName(configUtil.getAppId(), false);
        this.setUpstreamRepository(upstream);
    }

    public K8sConfigMapConfigRepository(String namespace, ConfigRepository upstream) {
        this.namespace = namespace;
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        kubernetesManager = ApolloInjector.getInstance(KubernetesManager.class);
        k8sNamespace = configUtil.getK8sNamespace();

        this.setConfigMapKey(configUtil.getCluster(), namespace);
        this.setConfigMapName(configUtil.getAppId(), false);
        this.setUpstreamRepository(upstream);
    }

    public String getConfigMapKey() {
        return configMapKey;
    }

    public String getConfigMapName() {
        return configMapName;
    }

    void setConfigMapKey(String cluster, String namespace) {
        // cluster: 用户定义>idc>default
        if (StringUtils.isBlank(cluster)) {
            configMapKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join("default", namespace);
            return;
        }
        configMapKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join(cluster, namespace);
    }

    void setConfigMapName(String appId, boolean syncImmediately) {
        Preconditions.checkNotNull(appId, "AppId cannot be null");
        configMapName = ConfigConsts.APOLLO_CONFIG_CACHE + appId;
        // 初始化configmap
        this.checkConfigMapName(configMapName);
        if (syncImmediately) {
            this.sync();
        }
    }

    private void checkConfigMapName(String configMapName) {
        if (StringUtils.isBlank(configMapName)) {
            throw new IllegalArgumentException("ConfigMap name cannot be null");
        }
        if (kubernetesManager.checkConfigMapExist(k8sNamespace, configMapName)) {
            return;
        }
        // Create an empty configmap, write the new value in the update event
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "createK8sConfigMap");
        transaction.addData("configMapName", configMapName);
        try {
            kubernetesManager.createConfigMap(k8sNamespace, configMapName, null);
            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
            transaction.setStatus(ex);
            throw new ApolloConfigException("Create configmap failed!", ex);
        } finally {
            transaction.complete();
        }
    }

    @Override
    public Properties getConfig() {
        if (configMapProperties == null) {
            sync();
        }
        Properties result = propertiesFactory.getPropertiesInstance();
        result.putAll(configMapProperties);
        logger.info("configmap properties: {}", configMapProperties);
        return result;
    }

    /**
     * Update the memory when the configuration center changes
     *
     * @param upstreamConfigRepository the upstream repo
     */
    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
        // 设置上游数据源
        if (upstreamConfigRepository == null) {
            return;
        }
        //clear previous listener
        if (upstream != null) {
            upstream.removeChangeListener(this);
        }
        upstream = upstreamConfigRepository;
        upstreamConfigRepository.addChangeListener(this);
    }

    @Override
    public ConfigSourceType getSourceType() {
        return sourceType;
    }

    /**
     * Sync the configmap
     */
    @Override
    protected void sync() {
        // Chain recovery, first read from upstream data source
        boolean syncFromUpstreamResultSuccess = trySyncFromUpstream();

        if (syncFromUpstreamResultSuccess) {
            return;
        }

        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncK8sConfigMap");
        Throwable exception = null;
        try {
            configMapProperties = loadFromK8sConfigMap();
            sourceType = ConfigSourceType.CONFIGMAP;
            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
            transaction.setStatus(ex);
            exception = ex;
            throw new ApolloConfigException("Load config from Kubernetes ConfigMap failed!", ex);
        } finally {
            transaction.complete();
        }

        if (configMapProperties == null) {
            sourceType = ConfigSourceType.NONE;
            throw new ApolloConfigException(
                    "Load config from Kubernetes ConfigMap failed!", exception);
        }
    }

    public Properties loadFromK8sConfigMap() {
        Preconditions.checkNotNull(configMapName, "ConfigMap name cannot be null");

        try {
            String jsonConfig = kubernetesManager.getValueFromConfigMap(k8sNamespace, configMapName, configMapKey);
            if (jsonConfig == null) {
                // TODO 这样重试访问idc，default是否正确
                String retryKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join("default", namespace);
                jsonConfig = kubernetesManager.getValueFromConfigMap(k8sNamespace, configMapName, retryKey);
            }

            // Convert jsonConfig to properties
            Properties properties = propertiesFactory.getPropertiesInstance();
            if (jsonConfig != null && !jsonConfig.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> configMap = gson.fromJson(jsonConfig, type);
                configMap.forEach(properties::setProperty);
            }
            return properties;
        } catch (Exception ex) {
            Tracer.logError(ex);
            throw new ApolloConfigException(String
                    .format("Load config from Kubernetes ConfigMap %s failed!", configMapName), ex);
        }
    }

    public boolean trySyncFromUpstream() {
        if (upstream == null) {
            return false;
        }
        try {
            logger.info("Start sync from the upstream data source, upstream.getConfig:{}, upstream.getSourceType():{}", upstream.getConfig(), upstream.getSourceType());
            updateConfigMapProperties(upstream.getConfig(), upstream.getSourceType());
            return true;
        } catch (Throwable ex) {
            Tracer.logError(ex);
            logger.warn("Sync config from upstream repository {} failed, reason: {}", upstream.getClass(),
                    ExceptionUtil.getDetailMessage(ex));
        }
        return false;
    }

    private synchronized void updateConfigMapProperties(Properties newProperties, ConfigSourceType sourceType) {
        this.sourceType = sourceType;
        if (newProperties.equals(configMapProperties)) {
            return;
        }
        this.configMapProperties = newProperties;
        persistConfigMap(configMapProperties);
    }

    /**
     * Update the memory
     *
     * @param namespace     the namespace of this repository change
     * @param newProperties the properties after change
     */
    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        if (newProperties.equals(configMapProperties)) {
            return;
        }
        Properties newFileProperties = propertiesFactory.getPropertiesInstance();
        newFileProperties.putAll(newProperties);
        updateConfigMapProperties(newFileProperties, upstream.getSourceType());
        this.fireRepositoryChange(namespace, newProperties);
    }

    public void persistConfigMap(Properties properties) {
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "persistK8sConfigMap");
        transaction.addData("configMapName", configMapName);
        transaction.addData("k8sNamespace", k8sNamespace);
        try {
            // Convert properties to a JSON string using Gson
            Gson gson = new Gson();
            String jsonConfig = gson.toJson(properties);
            Map<String, String> data = new HashMap<>();
            data.put(configMapKey, jsonConfig);

            // update configmap
            kubernetesManager.updateConfigMap(k8sNamespace, configMapName, data);
            transaction.setStatus(Transaction.SUCCESS);
        } catch (Exception ex) {
            ApolloConfigException exception =
                    new ApolloConfigException(
                            String.format("Persist config to Kubernetes ConfigMap %s failed!", configMapName), ex);
            Tracer.logError(exception);
            transaction.setStatus(exception);
            logger.error("Persist config to Kubernetes ConfigMap failed!", exception);
        } finally {
            transaction.complete();
        }
    }

}
