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

import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.github.dimabarbul.wiremock.openapi_validation.RequestBuilder.postJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.http.Response;

class AtlassianValidationResponseTransformerTest extends ValidationResponseTransformerTest {
    public AtlassianValidationResponseTransformerTest() {
        super("atlassian");
    }

    @Test
    void testGloballyIgnoreSpecificErrors() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                .withIgnoredErrors(List.of("validation.request.body.schema.required", "validation.response.status.unknown"))));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(post(ADD_USER_URL)
                .willReturn(jsonResponse("{}", HttpStatus.SC_OK)));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    void testIgnoreErrors() {
        wm.stubFor(post(ADD_USER_URL)
                .willReturn(jsonResponse("{}", HttpStatus.SC_OK)
                        .withTransformerParameter(
                                "openapiValidationIgnoreErrors",
                                Map.of(
                                        "validation.request.body.schema.required", true,
                                        "validation.response.status.unknown", true))));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    void testIgnoreSpecificErrorsCanBeAddedToGlobalList() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                .withIgnoredErrors(List.of("validation.request.body.schema.required"))));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(post(ADD_USER_URL)
                .willReturn(jsonResponse("{}", HttpStatus.SC_OK)
                        .withTransformerParameter(
                                "openapiValidationIgnoreErrors",
                                Map.of("validation.response.status.unknown", true))));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    void testGloballyIgnoredErrorCanBeEnabledWithTransformerParameters() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                .withIgnoredErrors(List.of("validation.request.body.schema.required", "validation.response.status.unknown"))));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(post(ADD_USER_URL)
                .willReturn(jsonResponse("{}", HttpStatus.SC_OK)
                        .withTransformerParameter(
                                "openapiValidationIgnoreErrors",
                                Map.of("validation.response.status.unknown", false))));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).doesNotContain("validation.request.body.schema.required");
        assertThat(response.getBodyAsString()).contains("validation.response.status.unknown");
    }
}
