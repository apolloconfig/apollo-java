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

import static com.ctrip.framework.apollo.monitor.internal.ApolloClientMonitorConstant.*;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class LocalFileConfigRepository extends AbstractConfigRepository
    implements RepositoryChangeListener {
  private static final Logger logger = DeferredLoggerFactory.getLogger(LocalFileConfigRepository.class);
  private static final String CONFIG_DIR = "/config-cache";
  private final String appId;
  private final String namespace;
  private File baseDir;
  private final ConfigUtil configUtil;
  private volatile Properties fileProperties;
  private volatile ConfigRepository upstream;

  private volatile ConfigSourceType sourceType = ConfigSourceType.LOCAL;

  /**
   * Constructor.
   *
   * @param namespace the namespace
   */
  public LocalFileConfigRepository(String appId, String namespace) {
    this(appId, namespace, null);
  }

  public LocalFileConfigRepository(String appId, String namespace, ConfigRepository upstream) {
    this.appId = appId;
    this.namespace = namespace;
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    this.setLocalCacheDir(findLocalCacheDir(), false);
    this.setUpstreamRepository(upstream);
  }

  void setLocalCacheDir(File baseDir, boolean syncImmediately) {
    this.baseDir = baseDir;
    this.checkLocalConfigCacheDir(this.baseDir);
    if (syncImmediately) {
      this.trySync();
    }
  }

  private File findLocalCacheDir() {
    try {
      String defaultCacheDir = configUtil.getDefaultLocalCacheDir(appId);
      Path path = Paths.get(defaultCacheDir);
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
      if (Files.exists(path) && Files.isWritable(path)) {
        return new File(defaultCacheDir, CONFIG_DIR);
      }
    } catch (Throwable ex) {
      //ignore
    }

    return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
  }

  @Override
  public Properties getConfig() {
    if (fileProperties == null) {
      sync();
    }
    Properties result = propertiesFactory.getPropertiesInstance();
    result.putAll(fileProperties);
    return result;
  }

  @Override
  public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
    if (upstreamConfigRepository == null) {
      return;
    }
    //clear previous listener
    if (upstream != null) {
      upstream.removeChangeListener(this);
    }
    upstream = upstreamConfigRepository;
    upstreamConfigRepository.addChangeListener(this);
  }

  @Override
  public ConfigSourceType getSourceType() {
    return sourceType;
  }

  @Override
  public void onRepositoryChange(String namespace, Properties newProperties) {
    this.onRepositoryChange(appId, namespace, newProperties);
  }

  @Override
  public void onRepositoryChange(String appId, String namespace, Properties newProperties) {
    if (newProperties.equals(fileProperties)) {
      return;
    }
    Properties newFileProperties = propertiesFactory.getPropertiesInstance();
    newFileProperties.putAll(newProperties);
    updateFileProperties(newFileProperties, upstream.getSourceType());
    this.fireRepositoryChange(appId, namespace, newProperties);
  }

  @Override
  protected void sync() {
    //sync with upstream immediately
    boolean syncFromUpstreamResultSuccess = trySyncFromUpstream();

    if (syncFromUpstreamResultSuccess) {
      return;
    }

    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncLocalConfig");
    Throwable exception = null;
    try {
      transaction.addData("Basedir", baseDir.getAbsolutePath());
      fileProperties = this.loadFromLocalCacheFile(baseDir, appId, namespace);
      sourceType = ConfigSourceType.LOCAL;
      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      Tracer.logEvent(APOLLO_CONFIG_EXCEPTION, ExceptionUtil.getDetailMessage(ex));
      transaction.setStatus(ex);
      exception = ex;
      //ignore
    } finally {
      transaction.complete();
    }

    if (fileProperties == null) {
      sourceType = ConfigSourceType.NONE;
      throw new ApolloConfigException(
          "Load config from local config failed!", exception);
    }
  }

  private boolean trySyncFromUpstream() {
    if (upstream == null) {
      return false;
    }
    try {
      updateFileProperties(upstream.getConfig(), upstream.getSourceType());
      return true;
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger
          .warn("Sync config from upstream repository {} failed, reason: {}", upstream.getClass(),
              ExceptionUtil.getDetailMessage(ex));
    }
    return false;
  }

  private synchronized void updateFileProperties(Properties newProperties, ConfigSourceType sourceType) {
    this.sourceType = sourceType;
    if (newProperties.equals(fileProperties)) {
      return;
    }
    this.fileProperties = newProperties;
    persistLocalCacheFile(baseDir, appId, namespace);
  }

  private Properties loadFromLocalCacheFile(File baseDir, String appId, String namespace) throws IOException {
    Preconditions.checkNotNull(baseDir, "Basedir cannot be null");

    File file = assembleLocalCacheFile(baseDir, appId, namespace);
    Properties properties = null;

    if (file.isFile() && file.canRead()) {
      InputStream in = null;

      try {
        in = new FileInputStream(file);
        properties = propertiesFactory.getPropertiesInstance();
        properties.load(in);
        logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());
      } catch (IOException ex) {
        Tracer.logError(ex);
        throw new ApolloConfigException(String
            .format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
      } finally {
        try {
          if (in != null) {
            in.close();
          }
        } catch (IOException ex) {
          // ignore
        }
      }
    } else {
      throw new ApolloConfigException(
          String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
    }

    return properties;
  }

  void persistLocalCacheFile(File baseDir, String appId, String namespace) {
    if (baseDir == null) {
      return;
    }
    File file = assembleLocalCacheFile(baseDir, appId, namespace);

    OutputStream out = null;

    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "persistLocalConfigFile");
    transaction.addData("LocalConfigFile", file.getAbsolutePath());
    try {
      out = new FileOutputStream(file);
      fileProperties.store(out, "Persisted by DefaultConfig");
      transaction.setStatus(Transaction.SUCCESS);
    } catch (IOException ex) {
      ApolloConfigException exception =
          new ApolloConfigException(
              String.format("Persist local cache file %s failed", file.getAbsolutePath()), ex);
      Tracer.logError(exception);
      transaction.setStatus(exception);
      logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(),
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          //ignore
        }
      }
      transaction.complete();
    }
  }

  private void checkLocalConfigCacheDir(File baseDir) {
    if (baseDir.exists()) {
      return;
    }
    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "createLocalConfigDir");
    transaction.addData("BaseDir", baseDir.getAbsolutePath());
    try {
      Files.createDirectory(baseDir.toPath());
      transaction.setStatus(Transaction.SUCCESS);
    } catch (IOException ex) {
      ApolloConfigException exception =
          new ApolloConfigException(
              String.format("Create local config directory %s failed", baseDir.getAbsolutePath()),
              ex);
      Tracer.logError(exception);
      transaction.setStatus(exception);
      logger.warn(
          "Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.",
          baseDir.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
    } finally {
      transaction.complete();
    }
  }

  File assembleLocalCacheFile(File baseDir, String appId, String namespace) {
    String fileName =
        String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
            .join(appId, configUtil.getCluster(), namespace));
    return new File(baseDir, fileName);
  }
}
