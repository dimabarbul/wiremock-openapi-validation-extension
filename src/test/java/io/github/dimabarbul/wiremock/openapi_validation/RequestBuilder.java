/*
 * Copyright 2024 Dmitriy Barbul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.dimabarbul.wiremock.openapi_validation;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.net.MediaType;

class RequestBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Request postJsonRequest(final String url, final Object body) {
        try {
            return ImmutableRequest.create()
                    .withMethod(RequestMethod.POST)
                    .withAbsoluteUrl(url)
                    .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                    .withBody(mapper.writeValueAsBytes(body))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Request postJsonRequest(final String url, final String body) {
            return ImmutableRequest.create()
                    .withMethod(RequestMethod.POST)
                    .withAbsoluteUrl(url)
                    .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                    .withBody(body.getBytes(StandardCharsets.UTF_8))
                    .build();
    }

    public static Request getRequest(final String url) {
        return ImmutableRequest.create()
                .withMethod(RequestMethod.GET)
                .withAbsoluteUrl(url)
                .build();
    }

    public static Request deleteRequest(final String url) {
        return ImmutableRequest.create()
                .withMethod(RequestMethod.DELETE)
                .withAbsoluteUrl(url)
                .build();
    }
}
