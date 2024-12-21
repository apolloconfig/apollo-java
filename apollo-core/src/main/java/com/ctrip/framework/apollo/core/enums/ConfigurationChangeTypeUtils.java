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
 * A utility class for the {@link ConfigurationChangeType} enum.
 * <p>
 * The class provides simple functionalities that extend the capabilities of
 * {@link ConfigurationChangeType}
 *
 * @author json
 */
public final class ConfigurationChangeTypeUtils {

  /**
   * Transforms a given String to its matching {@link ConfigurationChangeType}
   *
   * @param changeType the String to convert
   * @return the matching {@link ConfigurationChangeType} for the given String
   */
  public static ConfigurationChangeType transformChangeType(String changeType) {
    if (StringUtils.isBlank(changeType)) {
      return ConfigurationChangeType.UNKNOWN;
    }

    String cleanedChangeType = changeType.trim().toUpperCase();

    try {
      return ConfigurationChangeType.valueOf(cleanedChangeType);
    } catch (IllegalArgumentException e) {
      return ConfigurationChangeType.UNKNOWN;
    }
  }
}
