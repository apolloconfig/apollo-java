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

/**
 * Extra namespace info only returned when requesting with {@code extendInfo=true}.
 *
 * @since 2.6.0
 */
public class OpenNamespaceExtendDTO {

  private Boolean isConfigHidden;

  private String parentAppId;

  private Integer itemModifiedCnt;

  public Boolean getIsConfigHidden() {
    return isConfigHidden;
  }

  public void setIsConfigHidden(Boolean isConfigHidden) {
    this.isConfigHidden = isConfigHidden;
  }

  public String getParentAppId() {
    return parentAppId;
  }

  public void setParentAppId(String parentAppId) {
    this.parentAppId = parentAppId;
  }

  public Integer getItemModifiedCnt() {
    return itemModifiedCnt;
  }

  public void setItemModifiedCnt(Integer itemModifiedCnt) {
    this.itemModifiedCnt = itemModifiedCnt;
  }

  @Override
  public String toString() {
    return "OpenNamespaceExtendDTO{" + "isConfigHidden=" + isConfigHidden + ", parentAppId='"
        + parentAppId + '\'' + ", itemModifiedCnt=" + itemModifiedCnt + '}';
  }
}
