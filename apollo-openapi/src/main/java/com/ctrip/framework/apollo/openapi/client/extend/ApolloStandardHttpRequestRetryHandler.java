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

import org.apache.http.HttpRequest;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author zth9
 * @date 2025-05-07
 */
public class ApolloStandardHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

    private final Map<String, Boolean> idempotentMethods;

    public ApolloStandardHttpRequestRetryHandler(int retryCount) {
        super(retryCount, true, Arrays.asList(
            UnknownHostException.class,
            ConnectException.class,
            NoRouteToHostException.class,
            SSLException.class));
        this.idempotentMethods = new ConcurrentHashMap<>();
        this.idempotentMethods.put("GET", Boolean.TRUE);
        this.idempotentMethods.put("HEAD", Boolean.TRUE);
        this.idempotentMethods.put("PUT", Boolean.TRUE);
        this.idempotentMethods.put("DELETE", Boolean.TRUE);
        this.idempotentMethods.put("OPTIONS", Boolean.TRUE);
        this.idempotentMethods.put("TRACE", Boolean.TRUE);
    }

    @Override
    protected boolean handleAsIdempotent(final HttpRequest request) {
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        final Boolean b = this.idempotentMethods.get(method);
        return b != null && b.booleanValue();
    }
}
