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
package com.ctrip.framework.apollo.openapi.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Regression tests for the {@code extendInfo} default compatibility bridges added in 2.6.0.
 * <p>
 * {@link LegacyNamespaceOpenApiService} below only implements the pre-2.6.0 abstract contract
 * (the {@code fillItemDetail} overloads), exactly like an external implementation or test double
 * written before {@code extendInfo} was introduced. The fact that it compiles at all is itself
 * part of the regression being guarded against.
 */
public class NamespaceOpenApiServiceTest {

  @Test
  public void testGetNamespaceDefaultBridgeDelegatesToLegacyMethod() {
    LegacyNamespaceOpenApiService legacyService = new LegacyNamespaceOpenApiService();

    OpenNamespaceDTO result = legacyService.getNamespace("someAppId", "someEnv", "someCluster",
        "someNamespace", true, true);

    assertSame(legacyService.lastNamespaceResult, result);
    assertEquals(true, legacyService.lastFillItemDetail);
  }

  @Test
  public void testGetNamespacesDefaultBridgeDelegatesToLegacyMethod() {
    LegacyNamespaceOpenApiService legacyService = new LegacyNamespaceOpenApiService();

    List<OpenNamespaceDTO> result =
        legacyService.getNamespaces("someAppId", "someEnv", "someCluster", false, true);

    assertSame(legacyService.lastNamespacesResult, result);
    assertEquals(false, legacyService.lastFillItemDetail);
  }

  /**
   * A minimal stand-in for an implementation written before the {@code extendInfo} overloads
   * existed: it only overrides the {@code fillItemDetail} methods that were abstract prior to
   * 2.6.0, and never touches {@code extendInfo} at all.
   */
  private static class LegacyNamespaceOpenApiService implements NamespaceOpenApiService {

    private final OpenNamespaceDTO lastNamespaceResult = new OpenNamespaceDTO();
    private final List<OpenNamespaceDTO> lastNamespacesResult = Collections.emptyList();
    private boolean lastFillItemDetail;

    @Override
    public OpenNamespaceDTO getNamespace(String appId, String env, String clusterName,
        String namespaceName, boolean fillItemDetail) {
      this.lastFillItemDetail = fillItemDetail;
      return lastNamespaceResult;
    }

    @Override
    public List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName,
        boolean fillItemDetail) {
      this.lastFillItemDetail = fillItemDetail;
      return lastNamespacesResult;
    }

    @Override
    public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO) {
      throw new UnsupportedOperationException();
    }

    @Override
    public OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
        String namespaceName) {
      throw new UnsupportedOperationException();
    }
  }
}
