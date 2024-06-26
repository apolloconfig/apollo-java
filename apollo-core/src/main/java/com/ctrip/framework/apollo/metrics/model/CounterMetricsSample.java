package com.ctrip.framework.apollo.metrics.model;

import com.ctrip.framework.apollo.metrics.util.MeterType;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.Map;

/**
 * @author Rawven
 */
public class CounterMetricsSample extends MetricsSample {

  private AtomicDouble value;

  public CounterMetricsSample(String name, double num) {
    this.name = name;
    this.value = new AtomicDouble(num);
    this.type = MeterType.COUNTER;
  }

  public CounterMetricsSample(String name, double num,
      Map<String, String> tags) {
    this(name, 0);
    this.setTag(tags);
    this.value = new AtomicDouble(num);
    this.type = MeterType.COUNTER;
  }

  public Double getValue() {
    return value.get();
  }

  public void setValue(Double value) {
    this.value.set(value);
  }
}
