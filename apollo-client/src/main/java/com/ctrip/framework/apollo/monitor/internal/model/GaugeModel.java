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
package com.ctrip.framework.apollo.monitor.internal.model;

import com.ctrip.framework.apollo.monitor.internal.util.MeterType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class GaugeModel<T> extends MetricsModel {

  public static final ToDoubleFunction<Object> INT_CONVERTER = v -> (int) v;
  public static final ToDoubleFunction<Object> LONG_CONVERTER = v -> (long) v;
  public static final ToDoubleFunction<Object> DOUBLE_CONVERTER = v -> (double) v;

  private T value;
  private ToDoubleFunction<T> apply;

  public GaugeModel(String name, T value, ToDoubleFunction<T> apply) {
    this.name = name;
    this.value = value;
    this.apply = apply;
    this.type = MeterType.GAUGE;
  }

  public static <T> GaugeBuilder<T> builder() {
    return new GaugeBuilder<>();
  }

  public T getValue() {
    return value;
  }

  public void updateValue(T value) {
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

  public static class GaugeBuilder<T> {

    private final Map<String, String> tags = new HashMap<>(1);
    private String name;
    private T value;
    private ToDoubleFunction<T> apply;

    public GaugeBuilder<T> name(String name) {
      this.name = name;
      return this;
    }

    public GaugeBuilder<T> value(T value) {
      this.value = value;
      return this;
    }

    public GaugeBuilder<T> apply(ToDoubleFunction<T> apply) {
      this.apply = apply;
      return this;
    }

    public GaugeBuilder<T> putTag(String key, String value) {
      this.tags.put(key, value);
      return this;
    }

    public GaugeModel<T> build() {
      GaugeModel<T> sample = new GaugeModel<>(name, value, apply);
      sample.tags.putAll(tags);
      return sample;
    }
  }
}

