package com.ctrip.framework.apollo.metrics.collector;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.metrics.MetricsEvent;
import com.ctrip.framework.apollo.metrics.model.CounterMetricsSample;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import com.ctrip.framework.apollo.metrics.model.MetricsSample;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.ctrip.framework.apollo.tracer.internals.MetricsMessageProducer.TRACER_ERROR;
import static com.ctrip.framework.apollo.tracer.internals.MetricsMessageProducer.TRACER_EVENT;

public class TracerCollector implements MetricsCollector {
    private final List<ApolloConfigException> exceptions = new ArrayList<>();
    private final List<String> namespace404 = new ArrayList<>();
    private final List<String> namespaceTimeout = new ArrayList<>();

    public String getNamespace404() {
        return namespace404.toString();
    }

    public String getNamespaceTimeout() {
        return namespaceTimeout.toString();
    }

    public Integer getExceptionNum() {
        return exceptions.size();
    }

    @Override
    public boolean isSupport(String tag) {
        return TRACER_ERROR.equals(tag) || TRACER_EVENT.equals(tag);
    }

    @Override
    public void collect(MetricsEvent event) {
        switch (event.getTag()) {
            case TRACER_ERROR:
                solveError(event);
                break;
            case TRACER_EVENT:
                solveEvent(event);
                break;
            default:
                break;
        }
    }

    private void solveError(MetricsEvent event) {
        ApolloConfigException exception = (ApolloConfigException) event.getData();
        exceptions.add(exception);
    }

    @SuppressWarnings("all")
    private void solveEvent(MetricsEvent event) {
        String data = (String) event.getData();
        String[] split = data.split(":");
        String status = split[0];
        String namespace = split[1];
        if (status.equals("404")) {
            namespace404.add(namespace);
        } else if (status.equals("timeout")) {
            namespaceTimeout.add(namespace);
        }
    }

    @Override
    public boolean isSamplesUpdated() {
        return true;
    }

    @Override
    public List<MetricsSample> export() {
        List<MetricsSample> samples = new ArrayList<>();
        samples.add(new CounterMetricsSample("exceptionNum", exceptions.size()));
        HashMap<String, String> map = Maps.newHashMap();
        map.put("namespace404", namespace404.toString());
        samples.add(new GaugeMetricsSample<>("namespace404", namespace404.size(), value -> (double) namespace404.size(), map));
        HashMap<String, String> map1 = Maps.newHashMap();
        map1.put("namespaceTimeout", namespaceTimeout.toString());
        samples.add(new GaugeMetricsSample<>("namespaceTimeout", namespaceTimeout.size(), value -> (double) namespaceTimeout.size(), map1));
        return samples;
    }
}
