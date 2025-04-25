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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class InstanceOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private InstanceOpenApiService instanceOpenApiService;

  private String someAppId;
  private String someEnv;
  private String someCluster;
  private String someNamespace;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    someAppId = "someAppId";
    someEnv = "someEnv";
    someCluster = "someCluster";
    someNamespace = "someNamespace";
    instanceOpenApiService = new InstanceOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetInstanceCountByNamespace() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    StringEntity responseEntity = new StringEntity("1");
    when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    instanceOpenApiService.getInstanceCountByNamespace(someAppId, someEnv, someCluster, someNamespace);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String.format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances",
            someBaseUrl, someEnv, someAppId, someCluster, someNamespace), get.getURI().toString());
  }
}