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
package com.ctrip.framework.foundation.spi;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.foundation.spi.provider.Provider;

public interface ProviderManager extends Ordered {

  String getProperty(String name, String defaultValue);

  <T extends Provider> T provider(Class<T> clazz);

  /**
   * @since 2.3.0
   */
  default void initialize() {}

  @Override
  default int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
