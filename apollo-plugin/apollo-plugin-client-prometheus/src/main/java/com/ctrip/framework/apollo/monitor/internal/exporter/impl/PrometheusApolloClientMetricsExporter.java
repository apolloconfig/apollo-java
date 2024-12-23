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
package com.ctrip.framework.apollo.monitor.internal.exporter.impl;

import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.monitor.internal.exporter.AbstractApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter;
import com.ctrip.framework.apollo.monitor.internal.listener.impl.DefaultApolloClientNamespaceApi;
import com.google.common.collect.Maps;
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

/**
 * @author Rawven
 */
public class PrometheusApolloClientMetricsExporter extends
    AbstractApolloClientMetricsExporter implements ApolloClientMetricsExporter {

  private static final String PROMETHEUS = "prometheus";
  private final Logger logger = DeferredLoggerFactory.getLogger(
      PrometheusApolloClientMetricsExporter.class);
  protected CollectorRegistry registry;
  protected Map<String, Collector.Describable> map;

  @Override
  public void doInit() {
    registry = new CollectorRegistry();
    map = Maps.newConcurrentMap();
  }

  @Override
  public boolean isSupport(String form) {
    return PROMETHEUS.equals(form);
  }


  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags,
      double incrValue) {
    Counter counter = (Counter) map.computeIfAbsent(name,
        key -> createCounter(key, tags));
    counter.labels(tags.values().toArray(new String[0])).inc(incrValue);
  }

  private Counter createCounter(String name, Map<String, String> tags) {
    return Counter.build()
        .name(name)
        .help("apollo counter metrics")
        .labelNames(tags.keySet().toArray(new String[0]))
        .register(registry);
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    Gauge gauge = (Gauge) map.computeIfAbsent(name, key -> createGauge(key, tags));
    gauge.labels(tags.values().toArray(new String[0])).set(value);
  }

  private Gauge createGauge(String name, Map<String, String> tags) {
    return Gauge.build()
        .name(name)
        .help("apollo gauge metrics")
        .labelNames(tags.keySet().toArray(new String[0]))
        .register(registry);
  }


  @Override
  public String response() {
    try (StringWriter writer = new StringWriter()) {
      TextFormat.writeFormat(TextFormat.CONTENT_TYPE_OPENMETRICS_100, writer,
          registry.metricFamilySamples());
      return writer.toString();
    } catch (IOException e) {
      logger.error("Write metrics to Prometheus format failed", e);
      return "";
    }
  }
}
  