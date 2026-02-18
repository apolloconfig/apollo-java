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
package com.ctrip.framework.apollo.config.data.extension.webclient;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.util.http.HttpClient;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloWebClientHttpClient implements HttpClient {

  private static final Method CLIENT_RESPONSE_STATUS_CODE_METHOD = resolveClientResponseStatusCodeMethod();
  private static final ConcurrentMap<Class<?>, Method> STATUS_CODE_VALUE_METHOD_CACHE =
      new ConcurrentHashMap<Class<?>, Method>();

  private final WebClient webClient;

  private final Gson gson;

  public ApolloWebClientHttpClient(WebClient webClient) {
    this(webClient, new Gson());
  }

  public ApolloWebClientHttpClient(WebClient webClient, Gson gson) {
    this.webClient = webClient;
    this.gson = gson;
  }

  @Override
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, Class<T> responseType)
      throws ApolloConfigException {
    return this.doGetInternal(httpRequest, responseType);
  }

  private <T> HttpResponse<T> doGetInternal(HttpRequest httpRequest, Type responseType)
      throws ApolloConfigException {
    WebClient.RequestHeadersSpec<?> requestHeadersSpec = this.webClient.get()
        .uri(URI.create(httpRequest.getUrl()));
    if (!CollectionUtils.isEmpty(httpRequest.getHeaders())) {
      for (Map.Entry<String, String> entry : httpRequest.getHeaders().entrySet()) {
        requestHeadersSpec.header(entry.getKey(), entry.getValue());
      }
    }
    return requestHeadersSpec.exchangeToMono(clientResponse -> {
      int statusCode = this.resolveStatusCode(clientResponse);
      if (HttpStatus.OK.value() == statusCode) {
        return clientResponse.bodyToMono(String.class)
            .map(body -> new HttpResponse<T>(HttpStatus.OK.value(),
                gson.fromJson(body, responseType)));
      }
      if (HttpStatus.NOT_MODIFIED.value() == statusCode) {
        return Mono.just(new HttpResponse<T>(HttpStatus.NOT_MODIFIED.value(), null));
      }
      return Mono.error(new ApolloConfigStatusCodeException(statusCode,
          String.format("Get operation failed for %s", httpRequest.getUrl())));
    }).block();
  }

  @Override
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, Type responseType)
      throws ApolloConfigException {
    return this.doGetInternal(httpRequest, responseType);
  }

  /**
   * Resolve HTTP status code across Spring WebFlux 5/6/7.
   *
   * <p>ClientResponse#statusCode has different return types across major versions
   * (HttpStatus in Spring 5, HttpStatusCode in Spring 6/7). Calling it directly would bind
   * to one method descriptor at compile time and could fail on another runtime version.
   * Reflection keeps this bridge binary-compatible for Boot 2/3/4 compatibility tests.
   */
  private int resolveStatusCode(Object clientResponse) {
    try {
      Object statusCode = CLIENT_RESPONSE_STATUS_CODE_METHOD.invoke(clientResponse);
      if (statusCode == null) {
        throw new ApolloConfigException("Failed to resolve response status code: statusCode is null");
      }
      Method valueMethod = STATUS_CODE_VALUE_METHOD_CACHE.computeIfAbsent(statusCode.getClass(),
          ApolloWebClientHttpClient::resolveStatusCodeValueMethod);
      Object value = valueMethod.invoke(statusCode);
      return ((Number) value).intValue();
    } catch (Exception ex) {
      throw new ApolloConfigException("Failed to resolve response status code", ex);
    }
  }

  private static Method resolveClientResponseStatusCodeMethod() {
    try {
      return ClientResponse.class.getMethod("statusCode");
    } catch (NoSuchMethodException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static Method resolveStatusCodeValueMethod(Class<?> statusCodeType) {
    try {
      return statusCodeType.getMethod("value");
    } catch (NoSuchMethodException ex) {
      throw new IllegalStateException("Failed to resolve value() method from " + statusCodeType, ex);
    }
  }
}
