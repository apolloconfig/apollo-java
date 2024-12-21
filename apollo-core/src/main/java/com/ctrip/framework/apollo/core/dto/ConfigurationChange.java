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
package com.ctrip.framework.apollo.core.dto;


/**
 * Holds the information for a Configuration change.
 *
 * @author jason
 */
public class ConfigurationChange {

  private final String key;
  private final String newValue;
  private final String configurationChangeType;

  /**
   * Constructor.
   *
   * @param key                     the key whose value is changed
   * @param newValue                the value after change
   * @param configurationChangeType the change type
   */
  public ConfigurationChange(String key, String newValue, String configurationChangeType) {
    this.key = key;
    this.newValue = newValue;
    this.configurationChangeType = configurationChangeType;
  }

  public String getKey() {
    return key;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getConfigurationChangeType() {
    return configurationChangeType;
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfigChange{");
    sb.append(" key='").append(key).append('\'');
    sb.append(", newValue='").append(newValue).append('\'');
    sb.append(", configurationChangeType=").append(configurationChangeType);
    sb.append('}');
    return sb.toString();
  }
}
