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

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author zth9
 * @date 2025-05-07
 */
public class ApolloStandardHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

  private final Set<String> idempotentMethods;

  public ApolloStandardHttpRequestRetryHandler(int retryCount, IdempotentHttpMethod[] httpMethods) {
    super(retryCount, false, Arrays.asList(
        UnknownHostException.class,
        ConnectException.class,
        NoRouteToHostException.class,
        SSLException.class));
    this.idempotentMethods = new HashSet<>();
    if (httpMethods == null || httpMethods.length == 0) {
      // default set safe idempotent http method
      httpMethods = IdempotentHttpMethod.safe();
    }
    for (IdempotentHttpMethod httpMethod : httpMethods) {
      if (httpMethod == null) {
        continue;
      }
      idempotentMethods.add(httpMethod.name());
    }
  }

  @Override
  protected boolean handleAsIdempotent(final HttpRequest request) {
    String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
    return idempotentMethods.contains(method);
  }
}
