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
package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.MetaDomainConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.internals.RemoteConfigLongPollService;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class BaseIntegrationTest {
  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  private static final String someAppName = "someAppName";
  private static final String someInstanceId = "someInstanceId";
  private int port;
  private String metaServiceUrl;
  private String configServiceURL;
  protected static String someAppId;
  protected static String someClusterName;
  protected static String someDataCenter;
  protected static int refreshInterval;
  protected static TimeUnit refreshTimeUnit;
  protected static boolean propertiesOrderEnabled;
  private MockedConfigService mockedConfigService;

  protected final String defaultNamespace = ConfigConsts.NAMESPACE_APPLICATION;

  private File configDir;


  protected MockedConfigService newMockedConfigService() {
    this.mockedConfigService = new MockedConfigService(port);
    this.mockedConfigService.init();
    this.mockMetaServer();
    return this.mockedConfigService;
  }

  protected void mockMetaServer() {
    mockMetaServer(false);
  }

  protected void mockMetaServer(boolean failedAtFirstTime) {
    final ServiceDTO someServiceDTO = new ServiceDTO();
    someServiceDTO.setAppName(someAppName);
    someServiceDTO.setInstanceId(someInstanceId);
    someServiceDTO.setHomepageUrl(configServiceURL);
    this.mockedConfigService.mockMetaServer(failedAtFirstTime, someServiceDTO);
  }

  public void mockConfigs(
      int mockedStatusCode,
      ApolloConfig apolloConfig
  ) {
    this.mockConfigs(false, mockedStatusCode, apolloConfig);
  }

  public void mockConfigs(
      boolean failedAtFirstTime,
      int mockedStatusCode,
      ApolloConfig apolloConfig
  ) {
    this.mockedConfigService.mockConfigs(
        failedAtFirstTime, mockedStatusCode, apolloConfig
    );
  }

  @BeforeEach
  public void setUp() throws Exception {
    someAppId = "1003171";
    someClusterName = "someClusterName";
    someDataCenter = "someDC";

    refreshInterval = 5;
    refreshTimeUnit = TimeUnit.MINUTES;
    propertiesOrderEnabled = false;

    port = findFreePort();
    metaServiceUrl = configServiceURL =  "http://localhost:" + port;

    System.setProperty(ConfigConsts.APOLLO_META_KEY, metaServiceUrl);
    ReflectionTestUtils.invokeMethod(MetaDomainConsts.class, "reset");

    MockConfigUtil mockConfigUtil = new MockConfigUtil();
    MockInjector.setInstance(ConfigUtil.class, mockConfigUtil);
    configDir = new File(mockConfigUtil.getDefaultLocalCacheDir(someAppId)+ "/config-cache");

    if (configDir.exists()) {
      configDir.delete();
    }
    configDir.mkdirs();
  }

  @AfterEach
  public void tearDown() throws Exception {
    // get the instance will trigger long poll task execute, so move it from setup to tearDown
    RemoteConfigLongPollService remoteConfigLongPollService
        = ApolloInjector.getInstance(RemoteConfigLongPollService.class);
    ReflectionTestUtils.invokeMethod(remoteConfigLongPollService, "stopLongPollingRefresh");
    recursiveDelete(configDir);

    //as ConfigService is singleton, so we must manually clear its container
    ConfigService.reset();
    MockInjector.reset();
    System.clearProperty(ConfigConsts.APOLLO_META_KEY);
    ReflectionTestUtils.invokeMethod(MetaDomainConsts.class, "reset");

    if (mockedConfigService != null) {
      mockedConfigService.close();
      mockedConfigService = null;
    }
  }

  private void recursiveDelete(File file) {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        recursiveDelete(f);
      }
    }
    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  protected void setRefreshInterval(int refreshInterval) {
    BaseIntegrationTest.refreshInterval = refreshInterval;
  }

  protected void setRefreshTimeUnit(TimeUnit refreshTimeUnit) {
    BaseIntegrationTest.refreshTimeUnit = refreshTimeUnit;
  }

  protected void setPropertiesOrderEnabled(boolean propertiesOrderEnabled) {
    BaseIntegrationTest.propertiesOrderEnabled = propertiesOrderEnabled;
  }

  protected void createLocalCachePropertyFile(Properties properties) {
    createLocalCachePropertyFile(defaultNamespace, properties);
  }

  protected void createLocalCachePropertyFile(String namespace, Properties properties) {
    String filename = assembleLocalCacheFileName(namespace);
    File file = new File(configDir, filename);
    try (FileOutputStream in = new FileOutputStream(file)) {
      properties.store(in, "Persisted by " + this.getClass().getSimpleName());
    } catch (IOException e) {
      throw new IllegalStateException("fail to save " + namespace + " to file", e);
    }
  }

  private String assembleLocalCacheFileName(String namespace) {
    return String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someClusterName, namespace));
  }

  protected ApolloConfig assembleApolloConfig(
      String namespace,
      String releaseKey,
      Map<String, String> configurations
  ) {
    ApolloConfig apolloConfig =
        new ApolloConfig(someAppId, someClusterName, namespace, releaseKey);

    apolloConfig.setConfigurations(configurations);

    return apolloConfig;
  }

  public static class MockConfigUtil extends ConfigUtil {

    @Override
    public String getAppId() {
      return someAppId;
    }

    @Override
    public String getCluster() {
      return someClusterName;
    }

    @Override
    public int getRefreshInterval() {
      return refreshInterval;
    }

    @Override
    public TimeUnit getRefreshIntervalTimeUnit() {
      return refreshTimeUnit;
    }

    @Override
    public Env getApolloEnv() {
      return Env.DEV;
    }

    @Override
    public String getDataCenter() {
      return someDataCenter;
    }

    @Override
    public int getLoadConfigQPS() {
      return 200;
    }

    @Override
    public int getLongPollQPS() {
      return 200;
    }

    @Override
    public String getDefaultLocalCacheDir() {
      return ClassLoaderUtil.getClassPath();
    }

    @Override
    public long getOnErrorRetryInterval() {
      return 10;
    }

    @Override
    public TimeUnit getOnErrorRetryIntervalTimeUnit() {
      return TimeUnit.MILLISECONDS;
    }

    @Override
    public long getLongPollingInitialDelayInMills() {
      return 0;
    }

    @Override
    public boolean isPropertiesOrderEnabled() {
      return propertiesOrderEnabled;
    }

    @Override
    public String getDefaultLocalCacheDir(String appId) {
      String path = ClassLoaderUtil.getClassPath() + "/" + appId;
      if(isOSWindows()){
        // because there is an extra / in front of the windows system
        path = Paths.get(path.substring(1)).toString();
      }
      return path;
    }
  }

  /**
   * Returns a free port number on localhost.
   * <p>
   * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just
   * because of this). Slightly improved with close() missing in JDT. And throws exception instead
   * of returning -1.
   *
   * @return a free port number on localhost
   * @throws IllegalStateException if unable to find a free port
   */
  private static int findFreePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      int port = socket.getLocalPort();
      try {
        socket.close();
      } catch (IOException e) {
        // Ignore IOException on close()
      }
      return port;
    } catch (IOException e) {
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
        }
      }
    }
    throw new IllegalStateException(
        "Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
  }

}
