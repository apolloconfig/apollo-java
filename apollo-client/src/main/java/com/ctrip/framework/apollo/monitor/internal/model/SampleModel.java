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

import static com.ctrip.framework.apollo.monitor.internal.MonitorConstant.APOLLO_CLIENT;

import com.ctrip.framework.apollo.monitor.internal.util.MeterType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rawven
 */
public class SampleModel {

  private Map<String, String> tags;
  private String name;
  private MeterType type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = APOLLO_CLIENT + name;
  }

  public SampleModel putTag(String key, String value) {
    if (tags == null) {
      tags = new HashMap<>(1);
    }
    tags.put(key, value);
    return this;
  }

  public MeterType getType() {
    return type;
  }

  public void setType(MeterType type) {
    this.type = type;
  }

  public Map<String, String> getTags() {
    if (tags != null) {
      return Collections.unmodifiableMap(tags);
    }
    return null;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }
}

