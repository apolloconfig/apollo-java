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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.core.spi.Ordered;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface Injector extends Ordered {

  /**
   * Returns the appropriate instance for the given injection type
   */
  <T> T getInstance(Class<T> clazz);

  /**
   * Returns the appropriate instance for the given injection type and name
   */
  <T> T getInstance(Class<T> clazz, String name);

  @Override
  default int getOrder() {
    return 0;
  }
}
