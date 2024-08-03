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

/**
 * @author Rawven
 */
public class CounterModel extends SampleModel {

  private double nowValue;
  private double increaseValue;

  private CounterModel(String name, double num) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    if (Double.isNaN(num) || Double.isInfinite(num)) {
      throw new IllegalArgumentException("Number must be a valid double");
    }
    setName(name);
    setType(MeterType.COUNTER);
    this.nowValue = num;
    this.increaseValue = num;
  }

  public static CounterModel create(String name, double value) {
    return new CounterModel(name, value);
  }

  public void updateValue(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException("Value must be a valid double");
    }
    increaseValue = value - nowValue;
    nowValue = value;
  }

  public Double getIncreaseValue() {
    return increaseValue;
  }

  public void setValue(Double value) {
    if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException("Value must be a valid double");
    }
    this.nowValue = value;
  }
}
