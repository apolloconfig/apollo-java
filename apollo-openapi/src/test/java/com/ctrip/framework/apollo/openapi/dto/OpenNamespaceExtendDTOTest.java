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

import org.junit.Test;

public class OpenNamespaceExtendDTOTest {

  @Test
  public void testGettersAndSetters() {
    OpenNamespaceExtendDTO dto = new OpenNamespaceExtendDTO();

    dto.setParentAppId("public-app");
    dto.setIsConfigHidden(true);
    dto.setItemModifiedCnt(5);

    assertEquals("public-app", dto.getParentAppId());
    assertEquals(Boolean.TRUE, dto.getIsConfigHidden());
    assertEquals(5, dto.getItemModifiedCnt().intValue());
  }

  @Test
  public void testDefaultValuesAreNull() {
    OpenNamespaceExtendDTO dto = new OpenNamespaceExtendDTO();

    assertNull(dto.getParentAppId());
    assertNull(dto.getIsConfigHidden());
    assertNull(dto.getItemModifiedCnt());
  }

  @Test
  public void testToStringContainsAllFields() {
    OpenNamespaceExtendDTO dto = new OpenNamespaceExtendDTO();
    dto.setParentAppId("public-app");
    dto.setIsConfigHidden(false);
    dto.setItemModifiedCnt(2);

    String result = dto.toString();

    assertTrue(result.contains("public-app"));
    assertTrue(result.contains("isConfigHidden=false"));
    assertTrue(result.contains("itemModifiedCnt=2"));
  }
}
