package com.ctrip.framework.apollo.metrics.collector;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.metrics.MetricsConstant;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rawven
 */
public class ConfigCollector implements MetricsCollector {

  public static final String CONFIG_MANAGER = "ConfigManager";
  public static final String NAMESPACE_USED_TIMES = "namespaceUsedTimes";
  private final Map<String, Config> m_configs;
  private final Map<String, Object> m_configLocks;
  private final Map<String, ConfigFile> m_configFiles;
  private final Map<String, Object> m_configFileLocks;
  private final Map<String, Integer> namespaceUsed = Maps.newConcurrentMap();

  public ConfigCollector(Map<String, Config> m_configs,
      Map<String, Object> m_configLocks,
      Map<String, ConfigFile> m_configFiles,
      Map<String, Object> m_configFileLocks) {
    this.m_configs = m_configs;
    this.m_configLocks = m_configLocks;
    this.m_configFiles = m_configFiles;
    this.m_configFileLocks = m_configFileLocks;
  }

  public List<String> getNamespaceUsed() {
    ArrayList<String> namespaces = Lists.newArrayList();
    m_configs.forEach((k, v) -> namespaces.add(k));
    return namespaces;
  }

  public String getAllNamespaceUsedTimes() {
    return namespaceUsed.toString();
  }

  @Override
  public boolean isSupport(String tag) {
    return CONFIG_MANAGER.equals(tag);
  }

  @Override
  public void collect(MetricsEvent event) {
    switch (event.getName()) {
      case NAMESPACE_USED_TIMES:
        String namespace = event.getAttachmentValue(MetricsConstant.NAMESPACE);
        namespaceUsed.put(namespace, namespaceUsed.getOrDefault(namespace, 0) + 1);
        break;
      default:
    }
  }

  @Override
  public boolean isSamplesUpdated() {
    //TODO
    return true;
  }

  @Override
  public List<MetricsSample> export() {
    List<MetricsSample> samples = new ArrayList<>();
    namespaceUsed.forEach((k, v) -> {
      samples.add(new GaugeMetricsSample<>("namespace_" + k + "_used_times", v, value -> value));
    });
    return samples;
  }
}
