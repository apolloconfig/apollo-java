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
package com.ctrip.framework.apollo.openapi.client.extend;

/**
 * @author zth9
 * @date 2025-05-11
 */
public enum IdempotentHttpMethod {
  GET, HEAD, PUT, DELETE, OPTIONS, TRACE;

  /**
   * Usually, these methods are idempotent
   */
  public static IdempotentHttpMethod[] safe() {
    return new IdempotentHttpMethod[]{GET, HEAD, OPTIONS, TRACE};
  }

  /**
   * Standard HTTP idempotent method. While PUT and DELETE are technically idempotent, repeated
   * requests can yield different responses—such as a 404 on a second delete
   */
  public static IdempotentHttpMethod[] standard() {
    return new IdempotentHttpMethod[]{GET, HEAD, PUT, DELETE, OPTIONS, TRACE};
  }
}
