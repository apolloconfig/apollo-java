package com.ctrip.framework.apollo.plugin.prometheus;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.metrics.reporter.AbstractMetricsReporter;
import com.ctrip.framework.apollo.metrics.model.CounterMetricsSample;
import com.ctrip.framework.apollo.metrics.model.GaugeMetricsSample;
import com.ctrip.framework.apollo.metrics.reporter.MetricsReporter;
import com.ctrip.framework.apollo.util.ConfigUtil;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusMetricReporter extends AbstractMetricsReporter implements MetricsReporter {
    private static final Logger logger = LoggerFactory.getLogger(
        PrometheusMetricReporter.class);
    private final CollectorRegistry registry;
    private final Map<String, Collector.Describable> map = new HashMap<>();
    private final String PROMETHEUS = "Prometheus";
    public PrometheusMetricReporter() {
        super();
        ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        if(!Objects.equals(configUtil.getMonitorProtocol(), PROMETHEUS)){
            throw new IllegalStateException("PrometheusMetricReporter is not enabled");
        }
        this.registry = new CollectorRegistry();
    }

    @Override
    public void doInit() {

    }

    @Override
    public void registerCounterSample(CounterMetricsSample sample) {
        String[][] tags = getTags(sample);
        Counter counter = (Counter) map.computeIfAbsent(sample.getName(), k -> Counter.build()
            .name(sample.getName())
            .help("apollo")
            .labelNames(tags[0])
            .register(registry));
        counter.labels(tags[1]).inc(sample.getValue() - counter.get());
    }

    @Override
    public void registerGaugeSample(GaugeMetricsSample<?> sample) {
        String[][] tags = getTags(sample);
        Gauge gauge = (Gauge) map.computeIfAbsent(sample.getName(), k -> Gauge.build()
            .name(sample.getName())
            .help("apollo")
            .labelNames(tags[0])
            .register(registry));
        gauge.labels(tags[1]).set(sample.getApplyValue());
    }


    @Override
    public String response() {
        StringWriter writer = new StringWriter();
        try {
            TextFormat.writeFormat(TextFormat.CONTENT_TYPE_OPENMETRICS_100, writer, registry.metricFamilySamples());
        } catch (IOException e) {
            logger.error("Write metrics to Prometheus format failed", e);
        }
        return writer.toString();
    }
}