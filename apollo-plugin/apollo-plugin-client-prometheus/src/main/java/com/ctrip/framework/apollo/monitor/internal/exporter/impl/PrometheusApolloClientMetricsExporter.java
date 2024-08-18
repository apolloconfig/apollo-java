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
      DefaultApolloClientNamespaceApi.class);
  private CollectorRegistry registry;
  private  Map<String, Collector.Describable> map;

  @Override
  public void doInit() {
    registry = new CollectorRegistry();
    map = new HashMap<>();
  }

  @Override
  public boolean isSupport(String form) {
    return PROMETHEUS.equals(form);
  }


  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags,
      double incrValue) {
    Counter counter = (Counter) map.get(name);
    if (counter == null) {
      counter = createCounter(name, tags);
      map.put(name, counter);
    }
    counter.labels(tags.values().toArray(new String[0])).inc(incrValue);
  }

  private Counter createCounter(String name, Map<String, String> tags) {
    return Counter.build()
        .name(name)
        .help("apollo")
        .labelNames(tags.keySet().toArray(new String[0]))
        .register(registry);
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    Gauge gauge = (Gauge) map.get(name);
    if (gauge == null) {
      gauge = createGauge(name, tags);
      map.put(name, gauge);
    }
    gauge.labels(tags.values().toArray(new String[0])).set(value);
  }

  private Gauge createGauge(String name, Map<String, String> tags) {
    return Gauge.build()
        .name(name)
        .help("apollo")
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
  