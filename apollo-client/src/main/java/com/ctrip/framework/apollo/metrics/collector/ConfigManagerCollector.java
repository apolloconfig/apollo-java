package com.ctrip.framework.apollo.metrics.collector;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigManagerCollector implements MetricsCollector {
    private final Map<String, Config> m_configs;
    private final Map<String, Object> m_configLocks;
    private final Map<String, ConfigFile> m_configFiles;
    private final Map<String, Object> m_configFileLocks;

    public ConfigManagerCollector(Map<String, Config> m_configs,
        Map<String, Object> m_configLocks,
        Map<String, ConfigFile> m_configFiles,
        Map<String, Object> m_configFileLocks) {
       this.m_configs = m_configs;
        this.m_configLocks = m_configLocks;
        this.m_configFiles = m_configFiles;
        this.m_configFileLocks = m_configFileLocks;
    }

    public List<String> getNamespaceUsed(){
        ArrayList<String> namespaces = Lists.newArrayList();
        m_configs.forEach((k, v) -> namespaces.add(k));
        return namespaces;
    }

    @Override
    public boolean isSupport(String tag) {
        return false;
    }

    @Override
    public void collect(MetricsEvent event) {

    }

    @Override
    public boolean isSamplesUpdated() {
        return false;
    }

    @Override
    public List<MetricsSample> export() {
        return Collections.emptyList();
    }
}
