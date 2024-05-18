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

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.MetaDomainConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

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
  protected Gson gson = new Gson();

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

    MockInjector.setInstance(ConfigUtil.class, new MockConfigUtil());
  }

  @AfterEach
  public void tearDown() throws Exception {
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

  protected void setRefreshInterval(int refreshInterval) {
    BaseIntegrationTest.refreshInterval = refreshInterval;
  }

  protected void setRefreshTimeUnit(TimeUnit refreshTimeUnit) {
    BaseIntegrationTest.refreshTimeUnit = refreshTimeUnit;
  }

  protected void setPropertiesOrderEnabled(boolean propertiesOrderEnabled) {
    BaseIntegrationTest.propertiesOrderEnabled = propertiesOrderEnabled;
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
