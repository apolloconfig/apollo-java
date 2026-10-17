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

import static org.junit.Assert.assertEquals;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests using a real OpenTelemetry SDK to verify exported metric values.
 */
public class OpenTelemetryApolloClientMetricsExporterIntegrationTest {

  private SdkMeterProvider meterProvider;
  private InMemoryMetricReader reader;
  private OpenTelemetryApolloClientMetricsExporter exporter;

  @Before
  public void setUp() {
    reader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build();
    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();
    GlobalOpenTelemetry.set(sdk);

    exporter = new OpenTelemetryApolloClientMetricsExporter();
    exporter.doInit();
  }

  @After
  public void tearDown() {
    GlobalOpenTelemetry.resetForTest();
    meterProvider.close();
  }

  private double latestGaugeValue(String metricName, String tagValue) {
    for (MetricData md : reader.collectAllMetrics()) {
      if (!metricName.equals(md.getName())) {
        continue;
      }
      return md.getDoubleGaugeData().getPoints().stream()
          .filter(p -> tagValue.equals(p.getAttributes()
              .get(io.opentelemetry.api.common.AttributeKey.stringKey("thread_pool_name"))))
          .findFirst()
          .orElseThrow(() -> new AssertionError("No gauge point found for metric '" + metricName
              + "' with tag value '" + tagValue + "', all metrics: " + reader.collectAllMetrics()))
          .getValue();
    }
    throw new AssertionError("Metric not found: " + metricName);
  }

  private double latestCounterValue(String metricName) {
    for (MetricData md : reader.collectAllMetrics()) {
      if (metricName.equals(md.getName())) {
        return md.getDoubleSumData().getPoints().stream().findFirst()
            .orElseThrow(() -> new AssertionError("No counter point for " + metricName)).getValue();
      }
    }
    throw new AssertionError("Metric not found: " + metricName);
  }

  @Test
  public void gaugeShouldReflectLatestValueAfterUpdates() {
    Map<String, String> tags = new HashMap<>();
    tags.put("thread_pool_name", "ConfigService");

    exporter.registerOrUpdateGaugeSample("apollo_client_thread_pool_queue_size", tags, 5.0);
    // force collection so subsequent update is a new delta
    reader.collectAllMetrics();
    exporter.registerOrUpdateGaugeSample("apollo_client_thread_pool_queue_size", tags, 10.0);
    reader.collectAllMetrics();
    exporter.registerOrUpdateGaugeSample("apollo_client_thread_pool_queue_size", tags, 3.0);

    assertEquals(3.0, latestGaugeValue("apollo_client_thread_pool_queue_size", "ConfigService"),
        0.0001);
  }

  @Test
  public void gaugeWithDifferentTagsShouldRecordSeparateSeries() {
    Map<String, String> tags1 = new HashMap<>();
    tags1.put("thread_pool_name", "ConfigService");
    Map<String, String> tags2 = new HashMap<>();
    tags2.put("thread_pool_name", "RemoteConfigLongPollService");

    exporter.registerOrUpdateGaugeSample("apollo_client_thread_pool_active_task_count", tags1, 1.0);
    exporter.registerOrUpdateGaugeSample("apollo_client_thread_pool_active_task_count", tags2, 7.0);

    assertEquals(1.0,
        latestGaugeValue("apollo_client_thread_pool_active_task_count", "ConfigService"), 0.0001);
    assertEquals(7.0, latestGaugeValue("apollo_client_thread_pool_active_task_count",
        "RemoteConfigLongPollService"), 0.0001);
  }

  @Test
  public void counterShouldAccumulateAcrossUpdates() {
    Map<String, String> tags = new HashMap<>();
    tags.put("namespace", "application");

    reader.collectAllMetrics();
    exporter.registerOrUpdateCounterSample("apollo_client_exception_num", tags, 2.0);
    reader.collectAllMetrics();
    exporter.registerOrUpdateCounterSample("apollo_client_exception_num", tags, 3.0);

    assertEquals(5.0, latestCounterValue("apollo_client_exception_num"), 0.0001);
  }
}
