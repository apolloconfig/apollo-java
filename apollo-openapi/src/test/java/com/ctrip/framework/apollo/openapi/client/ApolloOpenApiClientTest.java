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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloOpenApiClientTest {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Test
  public void testCreate() {
    String someUrl = "http://someUrl";
    String someToken = "someToken";

    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder().withPortalUrl(someUrl).withToken(someToken).build();

    assertEquals(someUrl, client.getPortalUrl());
    assertEquals(someToken, client.getToken());
  }

  @Test
  public void testCreateWithInvalidUrl() {
    String someInvalidUrl = "someInvalidUrl";
    String someToken = "someToken";

    assertThrows(
        IllegalArgumentException.class,
        () -> ApolloOpenApiClient.newBuilder().withPortalUrl(someInvalidUrl).withToken(someToken).build()
    );
  }

  @Test
  @Disabled("only for integration test")
  public void testCreateApp() {
    String someUrl = "http://localhost:8070";
//    String someToken = "0627b87948c30517157e8b2a9565e473b5a97323a50128f584838ed10559d3fd";
    String someToken = "9d0a241e9cb2300f302a875b1195340b2b6f56373cf5ca5d006a3f4e1a46b3ef";

    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
        .withPortalUrl(someUrl)
        .withToken(someToken)
        .withReadTimeout(200 * 1000)
        .withConnectTimeout(200 * 1000)
        .build();

    final String appId = "openapi-create-app";

    {
      OpenAppDTO openAppDTO = new OpenAppDTO();
      openAppDTO.setName("openapi create app 测试名字");
      openAppDTO.setAppId(appId);
      openAppDTO.setOwnerName("user-test-xxx1");
      openAppDTO.setOwnerEmail("user-test-xxx1@xxx.com");
      openAppDTO.setOrgId("orgId1");
      openAppDTO.setOrgName("orgName1");
      client.createApp(openAppDTO);
    }

    List<OpenAppDTO> list = client.getAppsByIds(Collections.singletonList(appId));
    for (OpenAppDTO openAppDTO : list) {
      log.info("{}", openAppDTO);
    }
  }

}
