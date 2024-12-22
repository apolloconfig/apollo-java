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
package com.ctrip.framework.apollo.core.enums;

import com.ctrip.framework.apollo.core.utils.StringUtils;

/**
 * This enum represents all the possible Configuration sync types
 *
 * @since 2.0.0
 */
public enum ConfigSyncType {
  FULL_SYNC("FullSync"), INCREMENTAL_SYNC("IncrementalSync"), UNKNOWN("Unknown");

  private final String value;

  ConfigSyncType(String value) {
    this.value = value;
  }

  /**
   * Transforms a given string to its matching {@link ConfigSyncType}.
   *
   * @param value the string that matches
   * @return the matching {@link ConfigSyncType}
   */
  public static ConfigSyncType fromString(String value) {
    if (StringUtils.isEmpty(value)) {
      return FULL_SYNC;
    }
    for (ConfigSyncType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  /**
   * @return The string representation of the given {@link ConfigSyncType}
   */
  public String getValue() {
    return value;
  }
}
