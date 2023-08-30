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

import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do not run in 'mvn clean test',
 * left code here for develop test,
 * you can run the method by ide.
 */
class ApolloOpenApiClientIntegrationTest {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final ApolloOpenApiClient client = newClient();

  private final String env = "DEV";
  private final String clusterName = "default";

  ApolloOpenApiClient newClient() {
    String someUrl = "http://localhost:8070";
//    String someToken = "0627b87948c30517157e8b2a9565e473b5a97323a50128f584838ed10559d3fd";
    String someToken = "9d0a241e9cb2300f302a875b1195340b2b6f56373cf5ca5d006a3f4e1a46b3ef";

    return ApolloOpenApiClient.newBuilder()
        .withPortalUrl(someUrl)
        .withToken(someToken)
        .withReadTimeout(2000 * 1000)
        .withConnectTimeout(2000 * 1000)
        .build();
  }

  void createApp(String appId, String ownerName, boolean assignAppRoleToSelf, String ... admins) {
    {
      OpenAppDTO app = new OpenAppDTO();
      app.setName("openapi create app 测试名字 " + appId);
      app.setAppId(appId);
      app.setOwnerName(ownerName);
      app.setOwnerEmail(ownerName + "@apollo.apollo");
      app.setOrgId("orgIdFromOpenapi");
      app.setOrgName("orgNameFromOpenapi");
      OpenCreateAppDTO req = new OpenCreateAppDTO();
      req.setApp(app);
      req.setAdmins(new HashSet<>(Arrays.asList(admins)));
      req.setAssignAppRoleToSelf(assignAppRoleToSelf);
      log.info("create app {}, ownerName {} assignAppRoleToSelf {}", appId, ownerName, assignAppRoleToSelf);
      client.createApp(req);
    }
  }

  @Test
  @Disabled("only for integration test")
  public void testCreateApp() {
    final String appId = "openapi-create-app1";
    final String ownerName = "user-test-xxx1";
    createApp(appId, ownerName, false, "user-test-xxx2", "user3");

    List<OpenAppDTO> list = client.getAppsByIds(Collections.singletonList(appId));
    assertEquals(1, list.size());
    OpenAppDTO openAppDTO = list.get(0);
    log.info("{}", openAppDTO);
    assertEquals(appId, openAppDTO.getAppId());
    assertEquals(ownerName, openAppDTO.getOwnerName());
  }


  @Test
  @Disabled("only for integration test")
  public void testCreateAppButHaveNoAppRole() {
    // create app
    final String appIdSuffix = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss")
    );
    final String appId = "openapi-create-app-" + appIdSuffix;
    final String ownerName = "test-create-release1";
    createApp(appId, ownerName, false, "user-test-xxx1", "user-test-xxx2");

    {
      List<OpenAppDTO> list = client.getAppsByIds(Collections.singletonList(appId));
      assertEquals(1, list.size());
      OpenAppDTO openAppDTO = list.get(0);
      log.info("{}", openAppDTO);
      assertEquals(appId, openAppDTO.getAppId());
      assertEquals(ownerName, openAppDTO.getOwnerName());
    }

    // create namespace
    final String namespaceName = "openapi-create-namespace";
    {
      OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
      dto.setName(namespaceName);
      dto.setAppId(appId);
      dto.setComment("create from openapi");
      dto.setDataChangeCreatedBy(ownerName);
      log.info("create namespace {} should fail because have no app role", namespaceName);
      assertThrows(RuntimeException.class, () -> client.createAppNamespace(dto));
    }
  }

  @Test
  @Disabled("only for integration test")
  public void testCreateAppThenCreateClusterCreateNamespaceThenRelease() {
    // create app
    final String appIdSuffix = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss")
    );
    final String appId = "openapi-create-app-" + appIdSuffix;
    final String ownerName = "test-create-release1";
    createApp(appId, ownerName, true, "user-test-xxx1", "user-test-xxx2");

    {
      List<OpenAppDTO> list = client.getAppsByIds(Collections.singletonList(appId));
      assertEquals(1, list.size());
      OpenAppDTO openAppDTO = list.get(0);
      log.info("{}", openAppDTO);
      assertEquals(appId, openAppDTO.getAppId());
      assertEquals(ownerName, openAppDTO.getOwnerName());
    }

    // create cluster
    final String clusterName = "cluster-openapi";
    {
      OpenClusterDTO dto = new OpenClusterDTO();
      dto.setAppId(appId);
      dto.setName(clusterName);
      dto.setDataChangeCreatedBy(ownerName);
      log.info("create cluster {}", clusterName);
      client.createCluster(env, dto);
    }

    // create namespace
    final String namespaceName = "openapi-create-namespace";
    {
      OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
      dto.setName(namespaceName);
      dto.setAppId(appId);
      dto.setComment("create from openapi");
      dto.setDataChangeCreatedBy(ownerName);
      log.info("create namespace {}", namespaceName);
      client.createAppNamespace(dto);
    }

    // modify
    // k1=v1
    {
      OpenItemDTO itemDTO = new OpenItemDTO();
      itemDTO.setKey("k1");
      itemDTO.setValue("v1");
      itemDTO.setDataChangeCreatedBy(ownerName);
      client.createOrUpdateItem(
          appId, env, clusterName, namespaceName, itemDTO
      );
    }
    // k2=v2
    {
      OpenItemDTO itemDTO = new OpenItemDTO();
      itemDTO.setKey("k2");
      itemDTO.setValue("v2");
      itemDTO.setDataChangeCreatedBy(ownerName);
      client.createOrUpdateItem(
          appId, env, clusterName, namespaceName, itemDTO
      );
    }

    // release namespace
    {
      NamespaceReleaseDTO dto = new NamespaceReleaseDTO();
      dto.setReleaseTitle("openapi-release");
      dto.setReleasedBy(ownerName);
      dto.setReleaseComment("test openapi release in " + LocalDateTime.now());
      log.info("release namespace {}", namespaceName);
      client.publishNamespace(appId, env, clusterName, namespaceName, dto);
    }

    // read then namespace
    {
      OpenNamespaceDTO namespaceDTO
          = client.getNamespace(appId, env, clusterName, namespaceName);
      List<OpenItemDTO> items = namespaceDTO.getItems();
      Map<String, String> map = new HashMap<>(16);
      for (OpenItemDTO item : items) {
        map.put(item.getKey(), item.getValue());
      }
      assertEquals(2, map.size());
      assertEquals("v1", map.get("k1"));
      assertEquals("v2", map.get("k2"));
      log.info("create app {} namespace {} and release {} success", appId, namespaceName, map);
    }
  }

}
