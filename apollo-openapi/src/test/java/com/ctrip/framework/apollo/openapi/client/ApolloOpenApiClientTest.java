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

import org.junit.jupiter.api.Test;

public class ApolloOpenApiClientTest {

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

    assertThrows(IllegalArgumentException.class,()->
    ApolloOpenApiClient.newBuilder().withPortalUrl(someInvalidUrl).withToken(someToken).build());
  }
}
