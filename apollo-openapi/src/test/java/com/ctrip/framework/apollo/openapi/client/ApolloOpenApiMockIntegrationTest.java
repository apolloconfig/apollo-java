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
package com.ctrip.framework.apollo.openapi.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenOrganizationDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Mock integration tests that verify OpenAPI client request/response chain.
 */
public class ApolloOpenApiMockIntegrationTest {

  private HttpServer server;
  private MockPortalHandler handler;

  @Before
  public void setUp() throws Exception {
    handler = new MockPortalHandler();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/openapi/v1/", handler);
    server.start();
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  public void shouldCallFindAppsWithAuthorizationHeaderAndQuery() throws Exception {
    handler.mock("GET", "/openapi/v1/apps", 200,
        "[{\"appId\":\"SampleApp\",\"name\":\"SampleApp\",\"ownerName\":\"apollo\"}]");
    String token = "ci-openapi-token";
    ApolloOpenApiClient client = newClient(token);

    List<OpenAppDTO> apps = client.getAppsByIds(Collections.singletonList("SampleApp"));
    CapturedRequest request = handler.awaitRequest(5, TimeUnit.SECONDS);

    assertNotNull(apps);
    assertEquals(1, apps.size());
    assertEquals("SampleApp", apps.get(0).getAppId());
    assertEquals("GET", request.method);
    assertEquals("/openapi/v1/apps", request.path);
    assertEquals("appIds=SampleApp", request.query);
    assertEquals(token, request.authorization);
  }

  @Test
  public void shouldSerializeCreateAppRequestBody() throws Exception {
    handler.mock("POST", "/openapi/v1/apps", 200, "");
    ApolloOpenApiClient client = newClient("create-token");

    OpenAppDTO app = new OpenAppDTO();
    app.setAppId("SampleApp");
    app.setName("SampleApp");
    app.setOwnerName("apollo");

    OpenCreateAppDTO request = new OpenCreateAppDTO();
    request.setApp(app);
    request.setAdmins(Collections.singleton("apollo"));
    request.setAssignAppRoleToSelf(true);
    client.createApp(request);

    CapturedRequest capturedRequest = handler.awaitRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", capturedRequest.method);
    assertEquals("/openapi/v1/apps", capturedRequest.path);
    assertEquals("create-token", capturedRequest.authorization);
    assertTrue(capturedRequest.body.contains("\"appId\":\"SampleApp\""));
    assertTrue(capturedRequest.body.contains("\"assignAppRoleToSelf\":true"));
    assertTrue(capturedRequest.body.contains("\"admins\":[\"apollo\"]"));
  }

  @Test
  public void shouldUseDefaultClusterAndNamespace() throws Exception {
    handler.mock("GET", "/openapi/v1/envs/DEV/apps/SampleApp/clusters/default/namespaces/application",
        200,
        "{\"appId\":\"SampleApp\",\"clusterName\":\"default\",\"namespaceName\":\"application\"}");
    ApolloOpenApiClient client = newClient("namespace-token");

    OpenNamespaceDTO namespaceDTO = client.getNamespace("SampleApp", "DEV", null, null, true);
    CapturedRequest request = handler.awaitRequest(5, TimeUnit.SECONDS);

    assertNotNull(namespaceDTO);
    assertEquals("SampleApp", namespaceDTO.getAppId());
    assertEquals("default", namespaceDTO.getClusterName());
    assertEquals("application", namespaceDTO.getNamespaceName());
    assertEquals("GET", request.method);
    assertEquals("/openapi/v1/envs/DEV/apps/SampleApp/clusters/default/namespaces/application",
        request.path);
    assertEquals("fillItemDetail=true", request.query);
    assertEquals("namespace-token", request.authorization);
  }

  @Test
  public void shouldParseOrganizations() throws Exception {
    handler.mock("GET", "/openapi/v1/organizations", 200,
        "[{\"orgId\":\"100001\",\"orgName\":\"Apollo Team\"}]");
    ApolloOpenApiClient client = newClient("org-token");

    List<OpenOrganizationDto> organizations = client.getOrganizations();
    CapturedRequest request = handler.awaitRequest(5, TimeUnit.SECONDS);

    assertNotNull(organizations);
    assertEquals(1, organizations.size());
    assertEquals("100001", organizations.get(0).getOrgId());
    assertEquals("Apollo Team", organizations.get(0).getOrgName());
    assertEquals("GET", request.method);
    assertEquals("/openapi/v1/organizations", request.path);
    assertEquals("org-token", request.authorization);
  }

  @Test
  public void shouldWrapServerErrorsAsRuntimeException() {
    handler.mock("GET", "/openapi/v1/apps", 500, "internal error");
    ApolloOpenApiClient client = newClient("error-token");

    try {
      client.getAllApps();
      fail("Expected RuntimeException to be thrown");
    } catch (RuntimeException ex) {
      assertTrue(ex.getMessage().contains("Load app information"));
      assertNotNull(ex.getCause());
    }
  }

  private ApolloOpenApiClient newClient(String token) {
    return ApolloOpenApiClient.newBuilder()
        .withPortalUrl(String.format("http://127.0.0.1:%d", server.getAddress().getPort()))
        .withToken(token)
        .build();
  }

  private static class MockPortalHandler implements HttpHandler {

    private final Map<String, MockResponse> responses = new ConcurrentHashMap<>();
    private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();

    void mock(String method, String path, int statusCode, String body) {
      responses.put(key(method, path), new MockResponse(statusCode, body));
    }

    CapturedRequest awaitRequest(long timeout, TimeUnit unit) throws InterruptedException {
      CapturedRequest request = requests.poll(timeout, unit);
      assertNotNull(request);
      return request;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();
      String query = exchange.getRequestURI().getQuery();
      String authorization = exchange.getRequestHeaders().getFirst("Authorization");
      String requestBody = readRequestBody(exchange.getRequestBody());
      requests.offer(new CapturedRequest(method, path, query, authorization, requestBody));

      MockResponse response = responses.get(key(method, path));
      if (response == null) {
        response = new MockResponse(404, "");
      }
      byte[] responseBody = response.body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
      exchange.sendResponseHeaders(response.statusCode, responseBody.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(responseBody);
      }
    }

    private String readRequestBody(InputStream inputStream) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[256];
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private String key(String method, String path) {
      return method + " " + path;
    }
  }

  private static class MockResponse {
    private final int statusCode;
    private final String body;

    private MockResponse(int statusCode, String body) {
      this.statusCode = statusCode;
      this.body = body;
    }
  }

  private static class CapturedRequest {

    private final String method;
    private final String path;
    private final String query;
    private final String authorization;
    private final String body;

    private CapturedRequest(
        String method,
        String path,
        String query,
        String authorization,
        String body) {
      this.method = method;
      this.path = path;
      this.query = query;
      this.authorization = authorization;
      this.body = body;
    }
  }
}
