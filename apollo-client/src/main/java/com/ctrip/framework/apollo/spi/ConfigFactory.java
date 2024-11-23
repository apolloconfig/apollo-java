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
package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigFactory {
  /**
   * Create the config instance for the namespace.
   *
   * @param namespace the namespace
   * @return the newly created config instance
   */
  Config create(String namespace);

  /**
   * Create the config instance for the appId and namespace.
   *
   * @param appId the appId
   * @param namespace the namespace
   * @return the newly created config instance
   */
  Config create(String appId, String namespace);

  /**
   * Create the config file instance for the namespace
   * @param namespace the namespace
   * @return the newly created config file instance
   */
  ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat);

  /**
   * Create the config file instance for the appId and namespace.
   * @param appId the appId
   * @param namespace the namespace
   * @return the newly created config file instance
   */
  ConfigFile createConfigFile(String appId, String namespace, ConfigFileFormat configFileFormat);
}
