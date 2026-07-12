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
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * OpenTelemetry implementation of Apollo client metrics exporter.
 * Only uses OpenTelemetry API layer (no SDK dependency).
 *
 * @author teaho2015@gmail.com
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
  private Map<String, LongCounter> counterMap;
  private Map<String, ObservableDoubleGauge> gaugeMap;
  private Map<String, AtomicReference<Double>> gaugeValueMap;
  private Map<String, Attributes> attributesCache;

  @Override
  public void doInit() {
    // Get meter from global OpenTelemetry
    meter = GlobalOpenTelemetry.get().getMeter(METER_NAME);
    // Initialize maps
    counterMap = new ConcurrentHashMap<>();
    gaugeMap = new ConcurrentHashMap<>();
    gaugeValueMap = new ConcurrentHashMap<>();
    attributesCache = new ConcurrentHashMap<>();
    logger.info("OpenTelemetry metrics exporter initialized with meter: {}", METER_NAME);
  }

  @Override
  public boolean isSupport(String form) {
    return OPENTELEMETRY.equals(form);
  }

  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags,
      double incrValue) {
    try {
      if (meter == null) {
        logger.warn("OpenTelemetry meter not initialized, skipping counter registration for '{}'", name);
        return;
      }

      LongCounter counter = counterMap.computeIfAbsent(name, this::createCounter);

      Attributes attributes = getOrCreateAttributes(tags);
      counter.add((long) incrValue, attributes);

      logger.debug("Updated OpenTelemetry counter '{}' with value: {}, tags: {}",
          name, incrValue, tags);
    } catch (Exception e) {
      logger.error("Failed to register or update OpenTelemetry counter '{}'", name, e);
    }
  }

  private LongCounter createCounter(String name) {
    LongCounterBuilder builder = meter.counterBuilder(name)
        .setDescription("Apollo counter metrics")
        .setUnit(COUNTER_UNIT);

    // Build the counter
    return builder.build();
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    if (meter == null) {
      logger.warn("OpenTelemetry meter not initialized, skipping gauge registration for '{}'", name);
      return;
    }

    // Store the gauge value
    String gaugeKey = getGaugeKey(name, tags);
    gaugeValueMap.put(gaugeKey, new AtomicReference<>(value));

    // Register gauge if not already registered
    gaugeMap.computeIfAbsent(gaugeKey, key -> createGauge(name, tags, gaugeKey));

    logger.debug("Updated OpenTelemetry gauge '{}' with value: {}, tags: {}",
      name, value, tags);
  }

  private ObservableDoubleGauge createGauge(String name, Map<String, String> tags, String gaugeKey) {
    Attributes attributes = getOrCreateAttributes(tags);

    DoubleGaugeBuilder gaugeBuilder = meter.gaugeBuilder(name)
        .setDescription("Apollo gauge metrics")
        .setUnit(GAUGE_UNIT);

    // Register callback for gauge value
    return gaugeBuilder.buildWithCallback(measurement -> {
      AtomicReference<Double> valueRef = gaugeValueMap.get(gaugeKey);
      if (valueRef != null) {
        Double value = valueRef.get();
        if (value != null) {
          measurement.record(value, attributes);
        }
      }
    });
  }

  private Attributes getOrCreateAttributes(Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return Attributes.empty();
    }

    // Create cache key from sorted tag entries for consistency
    String cacheKey = createCacheKey(tags);

    return attributesCache.computeIfAbsent(cacheKey, key -> {
      AttributesBuilder builder = Attributes.builder();
      tags.forEach(builder::put);
      return builder.build();
    });
  }

  private String createCacheKey(Map<String, String> tags) {
    // Sort keys to ensure consistent cache key
    return tags.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .reduce((a, b) -> a + ";" + b)
        .orElse("");
  }

  private String getGaugeKey(String name, Map<String, String> tags) {
    return name + ":" + createCacheKey(tags);
  }

  @Override
  public String response() {
    // Return simple status information since we're only using API layer
    int counterCount = counterMap != null ? counterMap.size() : 0;
    int gaugeCount = gaugeMap != null ? gaugeMap.size() : 0;
    int attributesCount = attributesCache != null ? attributesCache.size() : 0;

    String meterStatus = (meter != null) ? METER_NAME : "not initialized";

    return String.format(
        "OpenTelemetry metrics exporter status - Counters: %d, Gauges: %d, Cached attributes: %d, Meter: %s",
        counterCount, gaugeCount, attributesCount, meterStatus);
  }
}