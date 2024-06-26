package com.ctrip.framework.apollo.metrics.model;

import static com.ctrip.framework.apollo.metrics.util.MeterType.GAUGE;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * @author Rawven
 */
public class GaugeMetricsSample<T> extends MetricsSample {

  private T value;

  private ToDoubleFunction<T> apply;

  public GaugeMetricsSample(String name, T value, ToDoubleFunction<T> apply) {
    this.name = name;
    this.value = value;
    this.apply = apply;
    this.type = GAUGE;
  }

  public GaugeMetricsSample(String name, T value, ToDoubleFunction<T> apply,
      Map<String, String> tags) {
    this.name = name;
    this.value = value;
    this.apply = apply;
    this.type = GAUGE;
    this.setTag(tags);
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public ToDoubleFunction<T> getApply() {
    return this.apply;
  }

  public void setApply(ToDoubleFunction<T> apply) {
    this.apply = apply;
  }

  public double getApplyValue() {
    return getApply().applyAsDouble(getValue());
  }
}
