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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ctrip.framework.foundation.internals.provider;

import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.ctrip.framework.foundation.internals.Utils;
import com.ctrip.framework.foundation.internals.io.BOMInputStream;
import com.ctrip.framework.foundation.spi.provider.Provider;
import com.ctrip.framework.foundation.spi.provider.ServerProvider;
import org.slf4j.Logger;

public class DefaultServerProvider implements ServerProvider {

  private static final Logger logger = DeferredLoggerFactory.getLogger(DefaultServerProvider.class);

  static final String DEFAULT_SERVER_PROPERTIES_PATH_ON_LINUX = "/opt/settings/server.properties";
  static final String DEFAULT_SERVER_PROPERTIES_PATH_ON_WINDOWS = "C:/opt/settings/server.properties";
  private String env;
  private String dc;

  private final Properties serverProperties = new Properties();

  String getServerPropertiesPath() {
    final String serverPropertiesPath = getCustomizedServerPropertiesPath();

    if (!Strings.isNullOrEmpty(serverPropertiesPath)) {
      return serverPropertiesPath;
    }

    return Utils.isOSWindows() ? DEFAULT_SERVER_PROPERTIES_PATH_ON_WINDOWS
        : DEFAULT_SERVER_PROPERTIES_PATH_ON_LINUX;
  }

  private String getCustomizedServerPropertiesPath() {
    // 1. Get from System Property
    final String serverPropertiesPathFromSystemProperty = System
        .getProperty("apollo.path.server.properties");
    if (!Strings.isNullOrEmpty(serverPropertiesPathFromSystemProperty)) {
      return serverPropertiesPathFromSystemProperty;
    }

    // 2. Get from OS environment variable
    final String serverPropertiesPathFromEnvironment = System
        .getenv("APOLLO_PATH_SERVER_PROPERTIES");
    if (!Strings.isNullOrEmpty(serverPropertiesPathFromEnvironment)) {
      return serverPropertiesPathFromEnvironment;
    }

    // last, return null if there is no custom value
    return null;
  }

  @Override
  public void initialize() {
    try {
      File file = new File(this.getServerPropertiesPath());
      if (file.exists() && file.canRead()) {
        logger.info("Loading {}", file.getAbsolutePath());
        FileInputStream fis = new FileInputStream(file);
        initialize(fis);
        return;
      }

      initialize(null);
    } catch (Throwable ex) {
      logger.error("Initialize DefaultServerProvider failed.", ex);
    }
  }

  @Override
  public void initialize(InputStream in) {
    try {
      if (in != null) {
        try {
          serverProperties
              .load(new InputStreamReader(new BOMInputStream(in), StandardCharsets.UTF_8));
        } finally {
          in.close();
        }
      }

      initEnvType();
      initDataCenter();
    } catch (Throwable ex) {
      logger.error("Initialize DefaultServerProvider failed.", ex);
    }
  }

  @Override
  public String getDataCenter() {
    return dc;
  }

  @Override
  public boolean isDataCenterSet() {
    return dc != null;
  }

  @Override
  public String getEnvType() {
    return env;
  }

  @Override
  public boolean isEnvTypeSet() {
    return env != null;
  }

  @Override
  public String getProperty(String name, String defaultValue) {
    if ("env".equalsIgnoreCase(name)) {
      String val = getEnvType();
      return val == null ? defaultValue : val;
    }
    if ("dc".equalsIgnoreCase(name)) {
      String val = getDataCenter();
      return val == null ? defaultValue : val;
    }
    String val = serverProperties.getProperty(name, defaultValue);
    return val == null ? defaultValue : val.trim();
  }

  @Override
  public Class<? extends Provider> getType() {
    return ServerProvider.class;
  }

  private void initEnvType() {
    // 1. Try to get environment from JVM system property
    env = System.getProperty("env");
    if (!Utils.isBlank(env)) {
      env = env.trim();
      logger.info("Environment is set to [{}] by JVM system property 'env'.", env);
      return;
    }

    // 2. Try to get environment from OS environment variable
    env = System.getenv("ENV");
    if (!Utils.isBlank(env)) {
      env = env.trim();
      logger.info("Environment is set to [{}] by OS env variable 'ENV'.", env);
      return;
    }

    // 3. Try to get environment from file "server.properties"
    env = serverProperties.getProperty("env");
    if (!Utils.isBlank(env)) {
      env = env.trim();
      logger.info("Environment is set to [{}] by property 'env' in server.properties.", env);
      return;
    }

    // 4. Set environment to null.
    env = null;
    logger.info(
        "Environment is set to null. Because it is not available in either (1) JVM system property 'env', (2) OS env variable 'ENV' nor (3) property 'env' from the properties InputStream.");
  }

  private void initDataCenter() {
    // 1. Try to get environment from JVM system property
    dc = System.getProperty("idc");
    if (!Utils.isBlank(dc)) {
      dc = dc.trim();
      logger.info("Data Center is set to [{}] by JVM system property 'idc'.", dc);
      return;
    }

    // 2. Try to get idc from OS environment variable
    dc = System.getenv("IDC");
    if (!Utils.isBlank(dc)) {
      dc = dc.trim();
      logger.info("Data Center is set to [{}] by OS env variable 'IDC'.", dc);
      return;
    }

    // 3. Try to get idc from from file "server.properties"
    dc = serverProperties.getProperty("idc");
    if (!Utils.isBlank(dc)) {
      dc = dc.trim();
      logger.info("Data Center is set to [{}] by property 'idc' in server.properties.", dc);
      return;
    }

    // 4. Set Data Center to null.
    dc = null;
    logger.debug(
        "Data Center is set to null. Because it is not available in either (1) JVM system property 'idc', (2) OS env variable 'IDC' nor (3) property 'idc' from the properties InputStream.");
  }

  @Override
  public String toString() {
    return "environment [" + getEnvType() + "] data center [" + getDataCenter() + "] properties: "
        + serverProperties
        + " (DefaultServerProvider)";
  }
}
