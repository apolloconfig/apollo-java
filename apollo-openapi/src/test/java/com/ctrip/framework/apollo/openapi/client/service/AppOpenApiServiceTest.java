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
package com.ctrip.framework.apollo.openapi.client.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AppOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private AppOpenApiService appOpenApiService;

  private String someAppId;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    someAppId = "someAppId";

    StringEntity responseEntity = new StringEntity("[]");
    lenient().when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    appOpenApiService = new AppOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetEnvClusterInfo() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    appOpenApiService.getEnvClusterInfo(someAppId);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String
        .format("%s/apps/%s/envclusters", someBaseUrl, someAppId), get.getURI().toString());
  }

  @Test
  public void testGetEnvClusterInfoWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(500);
      assertThrows(RuntimeException.class,()->
    appOpenApiService.getEnvClusterInfo(someAppId));
  }

  @Test
  public void testCreateAppNullApp() throws Exception {
    OpenCreateAppDTO req = new OpenCreateAppDTO();
      assertThrows(RuntimeException.class,()->
    appOpenApiService.createApp(req));
  }

  @Test
  public void testCreateAppEmptyAppId() throws Exception {
    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(new OpenAppDTO());
      assertThrows(RuntimeException.class,()->
    appOpenApiService.createApp(req));
  }

  @Test
  public void testCreateAppEmptyAppName() throws Exception {
    OpenAppDTO app = new OpenAppDTO();
    app.setAppId("appId1");

    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(app);
      assertThrows(RuntimeException.class,()->
    appOpenApiService.createApp(req));
  }

  @Test
  public void testCreateAppFail() throws Exception {
    OpenAppDTO app = new OpenAppDTO();
    app.setAppId("appId1");
    app.setName("name1");

    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(app);
    req.setAdmins(new HashSet<>(Arrays.asList("user1", "user2")));

    when(statusLine.getStatusCode()).thenReturn(400);
      assertThrows(RuntimeException.class,()->
    appOpenApiService.createApp(req));
  }


  @Test
  public void testCreateAppSuccess() throws Exception {
    OpenAppDTO app = new OpenAppDTO();
    app.setAppId("appId1");
    app.setName("name1");

    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(app);
    req.setAdmins(new HashSet<>(Arrays.asList("user1", "user2")));

    when(statusLine.getStatusCode()).thenReturn(200);
    {
      BasicHttpEntity httpEntity = new BasicHttpEntity();
      httpEntity.setContentLength(0L);
      httpEntity.setContent(new ByteArrayInputStream(new byte[0]));
      when(someHttpResponse.getEntity()).thenReturn(httpEntity);
    }

    appOpenApiService.createApp(req);

    verify(someHttpResponse, atLeastOnce()).getEntity();
    verify(httpClient, atLeastOnce()).execute(argThat(request -> {
      if (!"POST".equals(request.getMethod())) {
        return false;
      }
        return request.getURI().toString().endsWith("apps");
    }));

  }
}
