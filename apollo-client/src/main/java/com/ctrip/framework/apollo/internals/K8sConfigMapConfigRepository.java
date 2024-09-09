package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Kubernetes.KubernetesManager;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author dyx1234
 */
public class K8sConfigMapConfigRepository extends AbstractConfigRepository implements RepositoryChangeListener {

    private static final Logger logger = DeferredLoggerFactory.getLogger(K8sConfigMapConfigRepository.class);
    private final String nameSpace;
    private String configMapName;
    private String configMapNamespace;
    private final ConfigUtil configUtil;
    private final KubernetesManager kubernetesManager;
    private volatile Properties fileProperties;
    private volatile ConfigRepository upstream;
    private volatile ConfigSourceType sourceType = ConfigSourceType.CONFIGMAP;


    /**
     * Constructor
     *
     * @param namespace the namespace
     */
    public K8sConfigMapConfigRepository(String namespace) {
        this(namespace, null);
    }

    public K8sConfigMapConfigRepository(String namespace, ConfigRepository upstream) {
        nameSpace = namespace;
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        kubernetesManager = ApolloInjector.getInstance(KubernetesManager.class);
        configMapNamespace = configUtil.getConfigMapNamespace();

        this.setConfigMapName(configUtil.getAppId(), false);
        this.setUpstreamRepository(upstream);
    }

    void setConfigMapName(String appId, boolean syncImmediately){
        this.configMapName = appId;
        if (syncImmediately) {
            this.sync();
        }
    }

    @Override
    public Properties getConfig() {
        if (fileProperties == null) {
            sync();
        }
        Properties result = propertiesFactory.getPropertiesInstance();
        result.putAll(fileProperties);
        return result;
    }

    /**
     * Update the memory when the configuration center changes
     * @param upstreamConfigRepository the upstream repo
     */
    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
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
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncK8sConfigMap");
        try {
            fileProperties = loadFromK8sConfigMap();
            sourceType = ConfigSourceType.CONFIGMAP;
            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
            transaction.setStatus(ex);
            sourceType = ConfigSourceType.NONE;
            throw new ApolloConfigException("Load config from Kubernetes ConfigMap failed!", ex);
        } finally {
            transaction.complete();
        }
    }

    // 职责明确: manager层进行序列化和解析，把key传进去
    // repo这里只负责更新内存, Properties和appConfig格式的兼容
    public Properties loadFromK8sConfigMap() throws IOException {
        Preconditions.checkNotNull(configMapName, "ConfigMap name cannot be null");
        Properties properties = null;
        try {
            Map<String, String> data = kubernetesManager.getFromConfigMap(configMapNamespace, configUtil.getAppId());
            properties = propertiesFactory.getPropertiesInstance();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String value = entry.getValue();
                if (value != null) {
                    value = new String(Base64.getDecoder().decode(value));
                }
                properties.setProperty(entry.getKey(), value);
            }
            return properties;
        } catch (Exception ex) {
            logger.error("Failed to load config from Kubernetes ConfigMap: {}", configMapName, ex);
            throw new IOException("Failed to load config from Kubernetes ConfigMap", ex);
        }
    }


    /**
     * Update the memory
     * @param namespace the namespace of this repository change
     * @param newProperties the properties after change
     */
    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        if (newProperties.equals(fileProperties)) {
            return;
        }
        Properties newFileProperties = propertiesFactory.getPropertiesInstance();
        newFileProperties.putAll(newProperties);
        updateUpstreamProperties(newFileProperties, upstream.getSourceType());
        this.fireRepositoryChange(namespace, newProperties);
    }

    private synchronized void updateUpstreamProperties(Properties newProperties, ConfigSourceType sourceType) {
        this.sourceType = sourceType;
        if (newProperties.equals(fileProperties)) {
            return;
        }
        this.fileProperties = newProperties;
        persistLocalCacheFile(fileProperties);
    }

    public void persistLocalCacheFile(Properties properties) {
        // 将Properties中的值持久化到configmap中，并使用事务管理
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "persistLocalCacheFile");
        try {
            Map<String, String> data = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                data.put(entry.getKey().toString(), Base64.getEncoder().encodeToString(entry.getValue().toString().getBytes()));
                kubernetesManager.updateConfigMap(configUtil.getConfigMapNamespace(), configUtil.getAppId(), data);
                transaction.addData("configMapName", configMapName);
                transaction.addData("configMapNamespace", configMapNamespace);
                transaction.setStatus(Transaction.SUCCESS);
            }
        } catch (Throwable ex) {
            Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
            transaction.setStatus(ex);
            throw new ApolloConfigException("Persist local cache file failed!", ex);
        }
        transaction.complete();
    }

}
