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
package com.ctrip.framework.apollo.core.utils;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.net.URLDecoder;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ClassLoaderUtil {
  private static final Logger logger = LoggerFactory.getLogger(ClassLoaderUtil.class);

  private static ClassLoader loader = Thread.currentThread().getContextClassLoader();
  private static String classPath = "";

  static {
    if (loader == null) {
      logger.warn("Using system class loader");
      loader = ClassLoader.getSystemClassLoader();
    }

    try {
      classPath = resolveClassPath(loader, null);
      if (Strings.isNullOrEmpty(classPath)) {
        classPath = getDefaultClassPath();
      }
    } catch (Throwable ex) {
      classPath = getDefaultClassPath();
      logger.warn("Failed to locate class path, fallback to user.dir: {}", classPath, ex);
    }
  }

  public static ClassLoader getLoader() {
    return loader;
  }

  public static String getClassPath() {
    return classPath;
  }

  static String resolveClassPath(ClassLoader classLoader, String defaultClassPath) throws Exception {
    URL url = classLoader.getResource("");
    if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
      return defaultClassPath;
    }

    String resolvedClassPath = URLDecoder.decode(url.getPath(), "utf-8");
    if (Strings.isNullOrEmpty(resolvedClassPath)) {
      return defaultClassPath;
    }
    return resolvedClassPath;
  }

  private static String getDefaultClassPath() {
    return System.getProperty("user.dir");
  }

  public static boolean isClassPresent(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException ex) {
      // ignore expected exception
      return false;
    } catch (LinkageError ex) {
      // unexpected error, need to let the user know the actual error
      logger.error("Failed to load class: {}", className, ex);
      return false;
    }
  }
}
