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
package com.ctrip.framework.apollo.config.data.extension.webclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloWebClientHttpClientTest {

  private HttpServer server;
  private ApolloWebClientHttpClient httpClient;

  @Before
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();
    httpClient = new ApolloWebClientHttpClient(WebClient.builder().build());
  }

  @After
  public void tearDown() {
    server.stop(0);
  }

  @Test
  public void testDoGetWith200AndHeaders() {
    AtomicReference<String> headerValue = new AtomicReference<>();
    server.createContext("/ok", exchange -> {
      headerValue.set(exchange.getRequestHeaders().getFirst("x-apollo-test"));
      writeResponse(exchange, 200, "{\"value\":\"v1\"}");
    });

    HttpRequest request = new HttpRequest(url("/ok"));
    request.setHeaders(Collections.singletonMap("x-apollo-test", "header-value"));
    HttpResponse<ResponseBody> response = httpClient.doGet(request, ResponseBody.class);

    assertEquals(200, response.getStatusCode());
    assertEquals("v1", response.getBody().value);
    assertEquals("header-value", headerValue.get());
  }

  @Test
  public void testDoGetWith304() {
    server.createContext("/not-modified", exchange -> writeResponse(exchange, 304, ""));

    HttpRequest request = new HttpRequest(url("/not-modified"));
    HttpResponse<ResponseBody> response = httpClient.doGet(request, ResponseBody.class);

    assertEquals(304, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  public void testDoGetWithUnexpectedStatusCode() {
    server.createContext("/error", exchange -> writeResponse(exchange, 500, "internal"));

    HttpRequest request = new HttpRequest(url("/error"));

    try {
      httpClient.doGet(request, ResponseBody.class);
      fail("Expected ApolloConfigStatusCodeException");
    } catch (ApolloConfigStatusCodeException ex) {
      assertEquals(500, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("Get operation failed"));
    }
  }

  private String url(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  private void writeResponse(HttpExchange exchange, int code, String body) throws IOException {
    byte[] data = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(code, data.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(data);
    }
  }

  private static class ResponseBody {
    private String value;
  }
}

