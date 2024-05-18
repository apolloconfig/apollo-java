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

import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.model.RequestDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wxq
 */
public class MockedConfigService implements AutoCloseable {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final Gson gson = new Gson();
  private volatile ClientAndServer server;

  private final int port;

  public MockedConfigService(int port) {
    log.info("custom MockedConfigService use port: {}", port);
    this.port = port;
  }

  public static void main(String[] args) {
    Runnable runnable = () -> {
      MockedConfigService mockedConfigService = new MockedConfigService(10000);
      mockedConfigService.init();
      mockedConfigService.mockMetaServer(
          true,
          new ServiceDTO()
      );
      mockedConfigService.mockConfigs(
          true,
          200,
          new ApolloConfig()
      );
      mockedConfigService.mockLongPollNotifications(
          false,
          1000, 200, Lists.newArrayList(new ApolloConfigNotification("someNamespace", 1))
      );
      mockedConfigService.mockConfigs(
          false,
          200,
          new ApolloConfig()
      );
      mockedConfigService.mockConfigs(
          false,
          200,
          null
      );
    };
    Thread thread = new Thread(runnable);
    thread.start();
    System.out.println("wait");
  }

  public void init() {
    this.server = ClientAndServer.startClientAndServer(port);
  }

  public void mockMetaServer(ServiceDTO ... serviceDTOList) {
    mockMetaServer(false, serviceDTOList);
  }

  /**
   * @param serviceDTOList apollo meta server's response
   */
  public void mockMetaServer(boolean failedAtFirstTime, ServiceDTO ... serviceDTOList) {
    final String path = "/services/config";
    RequestDefinition requestDefinition = HttpRequest.request("GET").withPath(path);

    // need clear
    server.clear(requestDefinition);

    if (failedAtFirstTime) {
      server.when(requestDefinition, Times.exactly(1))
          .respond(HttpResponse.response()
              .withStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
              .withContentType(MediaType.JSON_UTF_8)
          );
    }

    String body = gson.toJson(Lists.newArrayList(serviceDTOList));
    server.when(requestDefinition)
        .respond(HttpResponse.response()
            .withStatusCode(HttpServletResponse.SC_OK)
            .withContentType(MediaType.JSON_UTF_8)
            .withBody(body)
        );
  }

  public void mockConfigs(
      int mockedStatusCode,
      ApolloConfig apolloConfig
  ) {
    mockConfigs(false, mockedStatusCode, apolloConfig);
  }

  /**
   * @param failedAtFirstTime failed at first time
   * @param mockedStatusCode http status code
   * @param apolloConfig apollo config server's response
   */
  public void mockConfigs(
      boolean failedAtFirstTime,
      int mockedStatusCode,
      ApolloConfig apolloConfig
  ) {
    // cannot use /configs/* as the path, because mock server will treat * as a wildcard
    final String path = "/configs/.*";
    RequestDefinition requestDefinition = HttpRequest.request("GET").withPath(path);

    // need clear
    server.clear(requestDefinition);

    if (failedAtFirstTime) {
      server.when(requestDefinition, Times.exactly(1))
          .respond(HttpResponse.response()
              .withStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
              .withContentType(MediaType.JSON_UTF_8)
          );
    }

    String body = gson.toJson(apolloConfig);
    server.when(requestDefinition)
        .respond(HttpResponse.response()
            .withStatusCode(mockedStatusCode)
            .withContentType(MediaType.JSON_UTF_8)
            .withBody(body)
        );
  }

  public void mockLongPollNotifications(
      final long pollResultTimeOutInMS,
      final int statusCode,
      final List<ApolloConfigNotification> result
  ) {
    mockLongPollNotifications(
        false, pollResultTimeOutInMS, statusCode, result);
  }

  public void mockLongPollNotifications(
      final boolean failedAtFirstTime,
      final long pollResultTimeOutInMS,
      final int statusCode,
      final List<ApolloConfigNotification> result
  ) {
    // match all parameters
    final String path = "/notifications/v2?.*";
    RequestDefinition requestDefinition = HttpRequest.request("GET").withPath(path);

    // need clear
    server.clear(requestDefinition);

    if (failedAtFirstTime) {
      server.when(requestDefinition, Times.exactly(1))
          .respond(HttpResponse.response()
              .withStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
              .withContentType(MediaType.JSON_UTF_8)
          );
    }

    String body = gson.toJson(result);
    server.when(requestDefinition)
        .respond(HttpResponse.response()
            .withStatusCode(statusCode)
            .withContentType(MediaType.JSON_UTF_8)
            .withBody(body)
            .withDelay(TimeUnit.MILLISECONDS, pollResultTimeOutInMS)
        );
  }

  @Override
  public void close() throws Exception {
    if (this.server.isRunning()) {
      this.server.stop();
    }
  }
}
