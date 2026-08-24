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

import com.ctrip.framework.apollo.Apollo;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.enums.ConfigSyncType;
import com.ctrip.framework.apollo.core.enums.ConfigurationChangeType;
import com.ctrip.framework.apollo.core.schedule.ExponentialSchedulePolicy;
import com.ctrip.framework.apollo.core.schedule.SchedulePolicy;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.ctrip.framework.apollo.util.http.HttpClient;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class RemoteConfigRepository extends AbstractConfigRepository {
  private static final Logger logger = DeferredLoggerFactory.getLogger(RemoteConfigRepository.class);
  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
  private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
  private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
  private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();

  private final ConfigServiceLocator serviceLocator;
  private final HttpClient httpClient;
  private final ConfigUtil configUtil;
  private final RemoteConfigLongPollService remoteConfigLongPollService;
  private volatile AtomicReference<ApolloConfig> configCache;
  private final String appId;
  private final String namespace;
  protected final static ScheduledExecutorService executorService;
  private final AtomicReference<ServiceDTO> longPollServiceDto;
  private final AtomicReference<ApolloNotificationMessages> remoteMessages;
  private final RateLimiter loadConfigRateLimiter;
  private final AtomicBoolean configNeedForceRefresh;
  private final SchedulePolicy loadConfigFailSchedulePolicy;
  private static final Gson GSON = new Gson();

  static {
    executorService = Executors.newScheduledThreadPool(1,
        ApolloThreadFactory.create("RemoteConfigRepository", true));
  }

  /**
   * Constructor.
   *
   * @param appId the appId
   * @param namespace the namespace
   */
  public RemoteConfigRepository(String appId, String namespace) {
    this.appId = appId;
    this.namespace = namespace;
    configCache = new AtomicReference<>();
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    httpClient = ApolloInjector.getInstance(HttpClient.class);
    serviceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
    remoteConfigLongPollService = ApolloInjector.getInstance(RemoteConfigLongPollService.class);
    longPollServiceDto = new AtomicReference<>();
    remoteMessages = new AtomicReference<>();
    loadConfigRateLimiter = RateLimiter.create(configUtil.getLoadConfigQPS());
    configNeedForceRefresh = new AtomicBoolean(true);
    loadConfigFailSchedulePolicy = new ExponentialSchedulePolicy(configUtil.getOnErrorRetryInterval(),
        configUtil.getOnErrorRetryInterval() * 8);
    this.schedulePeriodicRefresh();
    this.scheduleLongPollingRefresh();
  }

  @Override
  public Properties getConfig() {
    if (configCache.get() == null) {
      long start = System.currentTimeMillis();
      this.sync();
      Tracer.logEvent(APOLLO_CLIENT_NAMESPACE_FIRST_LOAD_SPEND+":"+namespace,
          String.valueOf(System.currentTimeMillis() - start));
    }
    return transformApolloConfigToProperties(configCache.get());
  }

  @Override
  public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
    //remote config doesn't need upstream
  }

  @Override
  public ConfigSourceType getSourceType() {
    return ConfigSourceType.REMOTE;
  }

  private void schedulePeriodicRefresh() {
    logger.debug("Schedule periodic refresh with interval: {} {}",
        configUtil.getRefreshInterval(), configUtil.getRefreshIntervalTimeUnit());
    executorService.scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            Tracer.logEvent(APOLLO_CONFIGSERVICE, String.format("periodicRefresh: %s", namespace));
            logger.debug("refresh config for namespace: {}", namespace);
            trySync();
            Tracer.logEvent(APOLLO_CLIENT_VERSION, Apollo.VERSION);
          }
        }, configUtil.getRefreshInterval(), configUtil.getRefreshInterval(),
        configUtil.getRefreshIntervalTimeUnit());
  }

  @Override
  protected synchronized void sync() {
    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncRemoteConfig");

    try {
      ApolloConfig previous = configCache.get();
      ApolloConfig current = loadApolloConfig();

      //reference equals means HTTP 304
      if (previous != current) {
        logger.debug("Remote Config refreshed!");
        configCache.set(current);
        this.fireRepositoryChange(appId, namespace, this.getConfig());
      }

      if (current != null) {
        Tracer.logEvent(String.format(APOLLO_CLIENT_CONFIGS+"%s", current.getNamespaceName()),
            current.getReleaseKey());
      }

      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      transaction.setStatus(ex);
      throw ex;
    } finally {
      transaction.complete();
    }
  }

  private Properties transformApolloConfigToProperties(ApolloConfig apolloConfig) {
    Properties result = propertiesFactory.getPropertiesInstance();
    result.putAll(apolloConfig.getConfigurations());
    return result;
  }

  private ApolloConfig loadApolloConfig() {
    if (!loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
      //wait at most 5 seconds
      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException e) {
      }
    }
    String appId = this.appId;
    String cluster = configUtil.getCluster();
    String dataCenter = configUtil.getDataCenter();
    String secret = configUtil.getAccessKeySecret(appId);
    Tracer.logEvent(APOLLO_CLIENT_CONFIGMETA, STRING_JOINER.join(appId, cluster, namespace));
    int maxRetries = configNeedForceRefresh.get() ? 2 : 1;
    long onErrorSleepTime = 0; // 0 means no sleep
    Throwable exception = null;

    List<ServiceDTO> configServices = getConfigServices();
    String url = null;
    retryLoopLabel:
    for (int i = 0; i < maxRetries; i++) {
      List<ServiceDTO> randomConfigServices = Lists.newLinkedList(configServices);
      Collections.shuffle(randomConfigServices);
      //Access the server which notifies the client first
      if (longPollServiceDto.get() != null) {
        randomConfigServices.add(0, longPollServiceDto.getAndSet(null));
      }

      for (ServiceDTO configService : randomConfigServices) {
        if (onErrorSleepTime > 0) {
          logger.warn(
              "Load config failed, will retry in {} {}. appId: {}, cluster: {}, namespaces: {}",
              onErrorSleepTime, configUtil.getOnErrorRetryIntervalTimeUnit(), appId, cluster, namespace);

          try {
            configUtil.getOnErrorRetryIntervalTimeUnit().sleep(onErrorSleepTime);
          } catch (InterruptedException e) {
            //ignore
          }
        }

        url = assembleQueryConfigUrl(configService.getHomepageUrl(), appId, cluster, namespace,
                dataCenter, remoteMessages.get(), configCache.get());

        logger.debug("Loading config from {}", url);

        HttpRequest request = new HttpRequest(url);
        if (!StringUtils.isBlank(secret)) {
          Map<String, String> headers = Signature.buildHttpHeaders(url, appId, secret);
          request.setHeaders(headers);
        }

        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "queryConfig");
        transaction.addData("Url", url);
        try {

          HttpResponse<ApolloConfig> response = httpClient.doGet(request, ApolloConfig.class);
          configNeedForceRefresh.set(false);
          loadConfigFailSchedulePolicy.success();

          transaction.addData("StatusCode", response.getStatusCode());
          transaction.setStatus(Transaction.SUCCESS);

          if (response.getStatusCode() == 304) {
            logger.debug("Config server responds with 304 HTTP status code.");
            return configCache.get();
          }

          ApolloConfig result = response.getBody();

          if (result != null) {
            ConfigSyncType configSyncType = ConfigSyncType.fromString(result.getConfigSyncType());

            if (configSyncType == ConfigSyncType.INCREMENTAL_SYNC) {
              ApolloConfig previousConfig = configCache.get();
              Map<String, String> previousConfigurations =
                  (previousConfig != null) ? previousConfig.getConfigurations() : null;
              result.setConfigurations(
                  mergeConfigurations(previousConfigurations, result.getConfigurationChanges()));
            } else if (configSyncType == ConfigSyncType.UNKNOWN) {
              String message = String.format(
                  "Invalid config sync type - %s",
                  result.getConfigSyncType());
              throw new ApolloConfigException(message, exception);
            }

          }

          logger.debug("Loaded config for {}: {}", namespace, result);

          return result;
        } catch (ApolloConfigStatusCodeException ex) {
          ApolloConfigStatusCodeException statusCodeException = ex;
          //config not found
          if (ex.getStatusCode() == 404) {
            String message = String.format(
                "Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, " +
                    "please check whether the configs are released in Apollo!",
                appId, cluster, namespace);
            statusCodeException = new ApolloConfigStatusCodeException(ex.getStatusCode(),
                message);
            Tracer.logEvent(APOLLO_CLIENT_NAMESPACE_NOT_FOUND,namespace);

          }
          Tracer.logEvent(APOLLO_CONFIG_EXCEPTION, ExceptionUtil.getDetailMessage(statusCodeException));
          transaction.setStatus(statusCodeException);
          exception = statusCodeException;
          if(ex.getStatusCode() == 404) {
            break retryLoopLabel;
          }
        } catch (Throwable ex) {
          Tracer.logEvent(APOLLO_CONFIG_EXCEPTION, ExceptionUtil.getDetailMessage(ex));
          transaction.setStatus(ex);
          exception = ex;
        } finally {
          transaction.complete();
        }

        // if force refresh, do normal sleep, if normal config load, do exponential sleep
        onErrorSleepTime = configNeedForceRefresh.get() ? configUtil.getOnErrorRetryInterval() :
            loadConfigFailSchedulePolicy.fail();
      }

    }
    String message = String.format(
        "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
        appId, cluster, namespace, url);
    throw new ApolloConfigException(message, exception);
  }

  String assembleQueryConfigUrl(String uri, String appId, String cluster, String namespace,
                                String dataCenter, ApolloNotificationMessages remoteMessages, ApolloConfig previousConfig) {

    String path = "configs/%s/%s/%s";
    List<String> pathParams =
        Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster),
            pathEscaper.escape(namespace));
    Map<String, String> queryParams = Maps.newHashMap();

    if (previousConfig != null) {
      queryParams.put("releaseKey", queryParamEscaper.escape(previousConfig.getReleaseKey()));
    }

    if (!Strings.isNullOrEmpty(dataCenter)) {
      queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
    }

    String localIp = configUtil.getLocalIp();
    if (!Strings.isNullOrEmpty(localIp)) {
      queryParams.put("ip", queryParamEscaper.escape(localIp));
    }

    String label = configUtil.getApolloLabel();
    if (!Strings.isNullOrEmpty(label)) {
      queryParams.put("label", queryParamEscaper.escape(label));
    }

    if (remoteMessages != null) {
      queryParams.put("messages", queryParamEscaper.escape(GSON.toJson(remoteMessages)));
    }

    String pathExpanded = String.format(path, pathParams.toArray());

    if (!queryParams.isEmpty()) {
      pathExpanded += "?" + MAP_JOINER.join(queryParams);
    }
    if (!uri.endsWith("/")) {
      uri += "/";
    }
    return uri + pathExpanded;
  }

  private void scheduleLongPollingRefresh() {
    remoteConfigLongPollService.submit(appId, namespace, this);
  }

  public void onLongPollNotified(ServiceDTO longPollNotifiedServiceDto, ApolloNotificationMessages remoteMessages) {
    longPollServiceDto.set(longPollNotifiedServiceDto);
    this.remoteMessages.set(remoteMessages);
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        configNeedForceRefresh.set(true);
        trySync();
      }
    });
  }

  private List<ServiceDTO> getConfigServices() {
    List<ServiceDTO> services = serviceLocator.getConfigServices();
    if (services.isEmpty()) {
      throw new ApolloConfigException("No available config service");
    }

    return services;
  }

  Map<String, String> mergeConfigurations(Map<String, String> previousConfigurations,
      List<ConfigurationChange> configurationChanges) {
    Map<String, String> newConfigurations = new HashMap<>();

    if (previousConfigurations != null) {
      newConfigurations = Maps.newHashMap(previousConfigurations);
    }

    if (configurationChanges == null) {
      return newConfigurations;
    }

    for (ConfigurationChange change : configurationChanges) {
      switch (ConfigurationChangeType.fromString(change.getConfigurationChangeType())) {
        case ADDED:
        case MODIFIED:
          newConfigurations.put(change.getKey(), change.getNewValue());
          break;
        case DELETED:
          newConfigurations.remove(change.getKey());
          break;
        default:
          //do nothing
          break;
      }
    }

    return newConfigurations;
  }
}
