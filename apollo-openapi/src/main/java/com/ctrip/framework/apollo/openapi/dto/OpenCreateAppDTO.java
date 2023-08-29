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

import java.util.Set;

public class OpenCreateAppDTO {

  /**
   * when {@code assignAppRoleToSelf} is true,
   * you can do anything with the app by current token!
   */
  private boolean assignAppRoleToSelf;

  /**
   * The application owner has project administrator permission by default.
   * <p>
   * Administrators can create namespace, cluster, and assign user permissions
   */
  private Set<String> admins;

  private OpenAppDTO app;

  public boolean isAssignAppRoleToSelf() {
    return assignAppRoleToSelf;
  }

  public void setAssignAppRoleToSelf(boolean assignAppRoleToSelf) {
    this.assignAppRoleToSelf = assignAppRoleToSelf;
  }

  public Set<String> getAdmins() {
    return admins;
  }

  public void setAdmins(Set<String> admins) {
    this.admins = admins;
  }

  public OpenAppDTO getApp() {
    return app;
  }

  public void setApp(OpenAppDTO app) {
    this.app = app;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("OpenCreateAppDTO{");
    sb.append("assignAppRoleToSelf='").append(assignAppRoleToSelf).append('\'');
    sb.append(", admins='").append(admins).append('\'');
    sb.append(", app=").append(app);
    sb.append('}');
    return sb.toString();
  }
}
