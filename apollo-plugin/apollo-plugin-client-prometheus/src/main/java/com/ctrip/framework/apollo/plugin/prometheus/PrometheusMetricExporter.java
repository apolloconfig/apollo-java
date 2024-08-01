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
package com.ctrip.framework.apollo.plugin.prometheus;

import com.ctrip.framework.apollo.monitor.internal.model.CounterModel;
import com.ctrip.framework.apollo.monitor.internal.model.GaugeModel;
import com.ctrip.framework.apollo.monitor.internal.exporter.AbstractMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.MetricsExporter;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rawven
 */
public class PrometheusMetricExporter extends AbstractMetricsExporter implements MetricsExporter {

  private static final Logger logger = LoggerFactory.getLogger(
      PrometheusMetricExporter.class);
  private final CollectorRegistry registry;
  private final Map<String, Collector.Describable> map = new HashMap<>();
  private final String PROMETHEUS = "prometheus";

  public PrometheusMetricExporter() {
    this.registry = new CollectorRegistry();
  }

  @Override
  public void doInit() {

  }

  @Override
  public boolean isSupport(String form) {
    return PROMETHEUS.equals(form);
  }

  @Override
  public void registerCounterSample(CounterModel sample) {
    String[][] tags = getTags(sample);
    Counter counter;
    if (!map.containsKey(sample.getName())) {
      counter = Counter.build()
          .name(sample.getName())
          .help("apollo")
          .labelNames(tags[0])
          .register(registry);
      map.put(sample.getName(), counter);
    } else {
      counter = (Counter) map.get(sample.getName());
    }
    counter.labels(tags[1]).inc(sample.getIncreaseValue());
  }

  @Override
  public void registerGaugeSample(GaugeModel<?> sample) {
    String[][] tags = getTags(sample);
    Gauge gauge;
    if (!map.containsKey(sample.getName())) {
      gauge = Gauge.build()
          .name(sample.getName())
          .help("apollo")
          .labelNames(tags[0])
          .register(registry);
      map.put(sample.getName(), gauge);
    } else {
      gauge = (Gauge) map.get(sample.getName());
    }
    gauge.labels(tags[1]).set(sample.getApplyValue());
  }


  @Override
  public String response() {
    StringWriter writer = new StringWriter();
    try {
      TextFormat.writeFormat(TextFormat.CONTENT_TYPE_OPENMETRICS_100, writer,
          registry.metricFamilySamples());
    } catch (IOException e) {
      logger.error("Write metrics to Prometheus format failed", e);
    }
    return writer.toString();
  }
}