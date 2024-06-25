package com.ctrip.framework.apollo.metrics.collector;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import com.ctrip.framework.apollo.util.ConfigUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StartUpCollector implements MetricsCollector {
    private ConfigUtil configUtil;

    public StartUpCollector() {
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    }

    public String getAppId() {
        return configUtil.getAppId();
    }

    public String getCluster() {
        return configUtil.getCluster();
    }

    public String getApolloEnv() {
        return configUtil.getApolloEnv().name();
    }

    @Override
    public boolean isSupport(String tag) {
        return false;
    }

    @Override
    public void collect(MetricsEvent event) {
        return;
    }

    @Override
    public boolean isSamplesUpdated() {
        //TODO
        return true;
    }

    @Override
    public List<MetricsSample> export() {
        List<MetricsSample> samples = new ArrayList<>();
        HashMap<String, String> tag = new HashMap<>(3);
        tag.put("appId", getAppId());
        tag.put("Cluster", getCluster());
        tag.put("env", getApolloEnv());
        samples.add(new GaugeMetricsSample<>("startup_parameters", 1, value -> 1, tag));
        return samples;
    }
}
