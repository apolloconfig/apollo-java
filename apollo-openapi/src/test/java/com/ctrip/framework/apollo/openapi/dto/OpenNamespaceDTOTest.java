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
package com.ctrip.framework.apollo.openapi.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Test;

public class OpenNamespaceDTOTest {

  @Test
  public void testGettersAndSetters() {
    OpenNamespaceDTO dto = new OpenNamespaceDTO();
    List<OpenItemDTO> items = Collections.singletonList(new OpenItemDTO());
    OpenNamespaceExtendDTO extendInfo = new OpenNamespaceExtendDTO();

    dto.setAppId("someAppId");
    dto.setClusterName("someCluster");
    dto.setNamespaceName("someNamespace");
    dto.setComment("someComment");
    dto.setFormat("properties");
    dto.setPublic(true);
    dto.setItems(items);
    dto.setExtendInfo(extendInfo);

    assertEquals("someAppId", dto.getAppId());
    assertEquals("someCluster", dto.getClusterName());
    assertEquals("someNamespace", dto.getNamespaceName());
    assertEquals("someComment", dto.getComment());
    assertEquals("properties", dto.getFormat());
    assertTrue(dto.isPublic());
    assertEquals(items, dto.getItems());
    assertEquals(extendInfo, dto.getExtendInfo());
  }

  @Test
  public void testDefaultValuesAreNull() {
    OpenNamespaceDTO dto = new OpenNamespaceDTO();

    assertNull(dto.getAppId());
    assertNull(dto.getItems());
    assertNull(dto.getExtendInfo());
    assertEquals(false, dto.isPublic());
  }

  @Test
  public void testInheritedBaseDTOFields() {
    OpenNamespaceDTO dto = new OpenNamespaceDTO();
    Date now = new Date();

    dto.setDataChangeCreatedBy("someCreator");
    dto.setDataChangeLastModifiedBy("someModifier");
    dto.setDataChangeCreatedTime(now);
    dto.setDataChangeLastModifiedTime(now);

    assertEquals("someCreator", dto.getDataChangeCreatedBy());
    assertEquals("someModifier", dto.getDataChangeLastModifiedBy());
    assertEquals(now, dto.getDataChangeCreatedTime());
    assertEquals(now, dto.getDataChangeLastModifiedTime());
  }

  @Test
  public void testToStringContainsExtendInfo() {
    OpenNamespaceDTO dto = new OpenNamespaceDTO();
    dto.setAppId("someAppId");
    OpenNamespaceExtendDTO extendInfo = new OpenNamespaceExtendDTO();
    extendInfo.setParentAppId("public-app");
    dto.setExtendInfo(extendInfo);

    String result = dto.toString();

    assertTrue(result.contains("someAppId"));
    assertTrue(result.contains("public-app"));
  }
}
