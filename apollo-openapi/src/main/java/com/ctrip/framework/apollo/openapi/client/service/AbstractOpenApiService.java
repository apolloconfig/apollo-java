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
package com.ctrip.framework.apollo.openapi.client.service;

import com.ctrip.framework.apollo.openapi.client.exception.ApolloOpenApiException;
import com.ctrip.framework.apollo.openapi.client.url.OpenApiPathBuilder;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

abstract class AbstractOpenApiService {
  private final String baseUrl;

  protected final CloseableHttpClient client;
  protected final Gson gson;

  AbstractOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
    this.client = client;
    this.baseUrl = baseUrl;
    this.gson = gson;
  }

  protected CloseableHttpResponse get(OpenApiPathBuilder path) throws IOException {
    HttpGet get = new HttpGet(path.buildPath(baseUrl));

    return execute(get);
  }

  protected CloseableHttpResponse post(OpenApiPathBuilder path, Object entity) throws IOException {
    HttpPost post = new HttpPost(path.buildPath(baseUrl));

    return execute(post, entity);
  }

  protected CloseableHttpResponse put(OpenApiPathBuilder path, Object entity) throws IOException {
    HttpPut put = new HttpPut(path.buildPath(baseUrl));

    return execute(put, entity);
  }

  protected CloseableHttpResponse delete(OpenApiPathBuilder path) throws IOException {
    HttpDelete delete = new HttpDelete(path.buildPath(baseUrl));

    return execute(delete);
  }

  private CloseableHttpResponse execute(HttpEntityEnclosingRequestBase requestBase, Object entity) throws IOException {
    requestBase.setEntity(new StringEntity(gson.toJson(entity), ContentType.APPLICATION_JSON));

    return execute(requestBase);
  }

  private CloseableHttpResponse execute(HttpUriRequest request) throws IOException {

    addClientIP(request);
    
    CloseableHttpResponse response = client.execute(request);

    checkHttpResponseStatus(response);

    return response;
  }

  private void checkHttpResponseStatus(HttpResponse response) {
    if (response.getStatusLine().getStatusCode() == 200) {
      return;
    }

    StatusLine status = response.getStatusLine();
    String message = "";
    try {
      message = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      //ignore
    }

    throw new ApolloOpenApiException(status.getStatusCode(), status.getReasonPhrase(), message);
  }

  private void addClientIP(HttpUriRequest request) {
    ProtocolVersion protocolVersion = request.getProtocolVersion();
    request.addHeader(HttpHeaders.FORWARDED, String.format("for=%s; proto=%s", Foundation.net().getHostAddress(), protocolVersion.getProtocol()));
    request.addHeader(HttpHeaders.X_FORWARDED_FOR, Foundation.net().getHostAddress());
  }

  protected void checkNotNull(Object value, String name) {
    Preconditions.checkArgument(null != value, name + " should not be null");
  }

  protected void checkNotEmpty(String value, String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(value), name + " should not be null or empty");
  }

  protected void checkPage(int page) {
    Preconditions.checkArgument(page >= 0, "page should be positive or 0");
  }

  protected void checkSize(int size) {
    Preconditions.checkArgument(size > 0, "size should be positive number");
  }

}
