/*
 * Copyright 2024 Apollo Authors
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

import com.ctrip.framework.apollo.openapi.client.url.OpenApiPathBuilder;
import com.ctrip.framework.apollo.openapi.dto.OpenOrganizationDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import java.lang.reflect.Type;
import java.util.List;

public class OrganizationOpenApiService extends AbstractOpenApiService implements
        com.ctrip.framework.apollo.openapi.api.OrganizationOpenApiService{
    public OrganizationOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
        super(client, baseUrl, gson);
    }

    private static final Type ORGANIZATIONS_DTO_LIST_TYPE = new TypeToken<List<OpenOrganizationDto>>() {
    }.getType();

    @Override
    public List<OpenOrganizationDto> getOrganizations() {
        OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
                .customResource("/organizations");

        try (CloseableHttpResponse response = get(pathBuilder)) {
            return gson.fromJson(EntityUtils.toString(response.getEntity()), ORGANIZATIONS_DTO_LIST_TYPE);
        } catch (Throwable ex) {
            throw new RuntimeException("get organizations information failed", ex);
        }

    }
}

