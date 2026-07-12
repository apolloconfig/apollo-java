/*
 * Copyright 2026 Apollo Authors
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
import com.google.common.collect.Maps;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * OpenTelemetry implementation of Apollo client metrics exporter.
 * Only uses OpenTelemetry API layer (no SDK dependency).
 *
 * @author leon.he@walmart.com
 */
public class OpenTelemetryApolloClientMetricsExporter extends
    AbstractApolloClientMetricsExporter implements ApolloClientMetricsExporter {

  private static final String OPENTELEMETRY = "opentelemetry";
  private static final String METER_NAME = "apollo-client";
  private static final String COUNTER_UNIT = "1";
  private static final String GAUGE_UNIT = "1";

  private final Logger logger = DeferredLoggerFactory.getLogger(
      OpenTelemetryApolloClientMetricsExporter.class);

  private Meter meter;
  private Map<String, DoubleCounter> counterMap;
  private Map<String, ObservableDoubleGauge> gaugeMap;

  @Override
  public void doInit() {
    // Get meter from global OpenTelemetry
    meter = GlobalOpenTelemetry.get().getMeter(METER_NAME);
    // Initialize maps
    counterMap = new ConcurrentHashMap<>();
    gaugeMap = new ConcurrentHashMap<>();
    logger.info("OpenTelemetry metrics exporter initialized with meter: {}", METER_NAME);
  }

  @Override
  public boolean isSupport(String form) {
    return OPENTELEMETRY.equals(form);
  }

  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags,
                                            double incrValue) {
    if (meter == null) {
      logger.warn("OpenTelemetry meter not initialized, skipping counter registration for '{}'", name);
      return;
    }
    DoubleCounter counter = counterMap.computeIfAbsent(name,
        key -> meter.counterBuilder(name).setDescription("Apollo counter metrics").setUnit(COUNTER_UNIT).ofDoubles().build());
    Attributes attributes = getOrCreateAttributes(tags);
    counter.add(incrValue, attributes);

    if (logger.isDebugEnabled()) {
      logger.debug("Updated OpenTelemetry counter '{}' with value: {}, tags: {}", name, incrValue, tags);
    }
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    if (meter == null) {
      logger.warn("OpenTelemetry meter not initialized, skipping gauge registration for '{}'", name);
      return;
    }
    Attributes attributes = getOrCreateAttributes(tags);
    // Register gauge if not already registered
    gaugeMap.computeIfAbsent(name, key -> meter.gaugeBuilder(name)
        .setDescription("Apollo gauge metrics")
        .setUnit(GAUGE_UNIT)
        .buildWithCallback(measurement -> {
          measurement.record(value, attributes);
        }));

    if (logger.isDebugEnabled()) {
      logger.debug("Updated OpenTelemetry gauge '{}' with value: {}, tags: {}",
          name, value, tags);
    }
  }

  private Attributes getOrCreateAttributes(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return Attributes.empty();
    }
    AttributesBuilder builder = Attributes.builder();
    tags.forEach(builder::put);
    return builder.build();
  }


  @Override
  public String response() {
    // Return simple status information since we're only using API layer
    int counterCount = counterMap != null ? counterMap.size() : 0;
    int gaugeCount = gaugeMap != null ? gaugeMap.size() : 0;

    String meterStatus = (meter != null) ? METER_NAME : "not initialized";
    return String.format(
        "OpenTelemetry metrics exporter status - Counters: %d, Gauges: %d, Meter: %s",
        counterCount, gaugeCount, meterStatus);
  }
}