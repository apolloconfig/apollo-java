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

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.APOLLO_CLIENT;

import com.ctrip.framework.apollo.monitor.internal.enums.MeterEnums;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rawven
 */
public class SampleModel {

  protected final AtomicDouble value = new AtomicDouble();
  private final Map<String, String> tags = new HashMap<>(1);
  private String name;
  private MeterEnums type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = APOLLO_CLIENT + name;
  }

  public SampleModel putTag(String key, String value) {
    tags.put(key, value);
    return this;
  }

  public SampleModel putTags(Map<String, String> tags) {
    this.tags.putAll(tags);
    return this;
  }

  public MeterEnums getType() {
    return type;
  }

  public void setType(MeterEnums type) {
    this.type = type;
  }

  public Map<String, String> getTags() {
    return Collections.unmodifiableMap(tags);
  }

  public double getValue() {
    return value.get();
  }

  public void setValue(double value) {
    this.value.set(value);
  }
}

