/*
 * Copyright 2025 Dmitriy Barbul
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.dimabarbul.wiremock.openapi_validation.RequestBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

abstract class ValidationResponseTransformerTest {

    protected static final int DEFAULT_VALIDATION_FAILURE_STATUS_CODE = 500;
    protected static final String GET_USERS_URL = "/users";
    protected static final String ADD_USER_URL = "/users";
    protected static final String DELETE_USER_URL = "/users/123";
    protected static final String JSON_OPENAPI_FILE_PATH = "src/test/resources/openapi.json";
    protected static final String YAML_OPENAPI_FILE_PATH = "src/test/resources/openapi.yaml";
    protected static final String INVALID_OPENAPI_FILE_PATH = "src/test/resources/invalid_openapi.json";

    protected final DirectCallHttpServerFactory factory;
    protected final DirectCallHttpServer server;
    protected final WireMockServer wm;

    private final String validatorName;

    public ValidationResponseTransformerTest(final String validatorName) {
        this.validatorName = validatorName;
        factory = new DirectCallHttpServerFactory();
        wm = new WireMockServer(getDefaultWireMockConfiguration());
        server = factory.getHttpServer();
    }

    @Test
    void testRequestBodyHasRequiredProperties() {
        wm.stubFor(post(ADD_USER_URL).willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(
                wm.url(ADD_USER_URL),
                JsonNodeFactory.instance
                        .objectNode()
                        .put("id", UUID.randomUUID().toString())
                        .put("username", "root")
                        .put("role", "admin")));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_CREATED);
    }

    @Test
    void testRequestBodyMissesRequiredProperties() {
        wm.stubFor(post(ADD_USER_URL).willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    @Test
    void testRequestBodyHasInvalidProperties() {
        wm.stubFor(post(ADD_USER_URL).willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(
                wm.url(ADD_USER_URL),
                JsonNodeFactory.instance
                        .objectNode()
                        .put("id", "test")
                        .put("username", "toolongusername")
                        .put("name", "x")
                        .put("dob", "invalid date string")
                        .put("role", "unknown")
                        .put("extra", "extra")));

        assertResponseFailedBecauseOfValidation(response);
        assertAll(
                "Request validation errors",
                () -> assertThat(response.getBodyAsString())
                        .contains("[Path '/id'] Input string \"test\" is not a valid UUID"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/username'] String \"toolongusername\" is too long (length: 15, maximum allowed: 10)"),
                () -> assertThat(response.getBodyAsString())
                        .contains("[Path '/name'] String \"x\" is too short (length: 1, required minimum: 2)"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/dob'] String \"invalid date string\" is invalid against requested date format(s) yyyy-MM-dd"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/role'] Instance value (\"unknown\") not found in enum (possible values: [\"user\",\"admin\"])"),
                () -> assertThat(response.getBodyAsString())
                        .contains("Object instance has properties which are not allowed by the schema: [\"extra\"]"));
    }

    @Test
    void testRequestHasRequiredQueryStringParameter() {
        wm.stubFor(delete(urlPathEqualTo(DELETE_USER_URL))
                .withQueryParam("soft", equalTo("true"))
                .willReturn(noContent()));

        Response response = server.stubRequest(deleteRequest(wm.url(DELETE_USER_URL + "?soft=true")));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    void testRequestMissesRequiredQueryStringParameter() {
        wm.stubFor(delete(urlPathEqualTo(DELETE_USER_URL)).willReturn(noContent()));

        Response response = server.stubRequest(deleteRequest(wm.url(DELETE_USER_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString())
                .contains("Query parameter 'soft' is required on path '/users/{userId}' but not found in request.");
    }

    @Test
    void testResponseHasRequiredProperties() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse(
                        JsonNodeFactory.instance
                                .arrayNode()
                                .add(JsonNodeFactory.instance
                                        .objectNode()
                                        .put("id", UUID.randomUUID().toString())
                                        .put("username", "root")
                                        .put("role", "admin")),
                        HttpStatus.SC_OK)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    void testResponseMissesRequiredProperties() {
        wm.stubFor(get(GET_USERS_URL).willReturn(jsonResponse("[{}]", HttpStatus.SC_OK)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    @Test
    void testResponseHasInvalidProperties() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse(
                        JsonNodeFactory.instance
                                .arrayNode()
                                .add(JsonNodeFactory.instance
                                        .objectNode()
                                        .put("id", "test")
                                        .put("username", "toolongusername")
                                        .put("name", "x")
                                        .put("dob", "invalid date string")
                                        .put("role", "unknown")),
                        HttpStatus.SC_OK)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertAll(
                "Response validation errors",
                () -> assertThat(response.getBodyAsString())
                        .contains("[Path '/0/id'] Input string \"test\" is not a valid UUID"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/0/username'] String \"toolongusername\" is too long (length: 15, maximum allowed: 10)"),
                () -> assertThat(response.getBodyAsString())
                        .contains("[Path '/0/name'] String \"x\" is too short (length: 1, required minimum: 2)"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/0/dob'] String \"invalid date string\" is invalid against requested date format(s) yyyy-MM-dd"),
                () -> assertThat(response.getBodyAsString())
                        .contains(
                                "[Path '/0/role'] Instance value (\"unknown\") not found in enum (possible values: [\"user\",\"admin\"])"));
    }

    @Test
    void testResponseHasInvalidStatusCode() {
        wm.stubFor(get(GET_USERS_URL).willReturn(jsonResponse("", HttpStatus.SC_NOT_FOUND)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString())
                .contains("Response status 404 not defined for path '" + GET_USERS_URL + "'.");
    }

    @Test
    void testUnknownPath() {
        wm.stubFor(get(UrlPattern.ANY).willReturn(noContent()));

        String unknownPath = "/unknown";
        Response response = server.stubRequest(getRequest(wm.url(unknownPath)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString())
                .contains("No API path found that matches request '" + unknownPath + "'.");
    }

    @Test
    void testUnmatchedResponse() {
        wm.stubFor(get("/test").willReturn(noContent()));

        Response response = server.stubRequest(getRequest(wm.url("/not-test")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void testGlobalCustomResponseCodeOnValidationFailure() {
        WireMockServer wm = new WireMockServer(
                getWireMockConfiguration(ExtensionOptions.builder().withFailureStatusCode(599)));

        wm.stubFor(get(UrlPattern.ANY).willReturn(noContent()));

        DirectCallHttpServer server = factory.getHttpServer();

        Response response = server.stubRequest(getRequest(wm.url("/test")));

        assertResponseFailedBecauseOfValidation(response, 599);
    }

    @Test
    void testCustomResponseCodeOnValidationFailure() {
        int statusCode = 598;
        wm.stubFor(get(UrlPattern.ANY)
                .willReturn(noContent().withTransformerParameter("openapiValidationFailureStatusCode", statusCode)));

        Response response = server.stubRequest(getRequest(wm.url("/test")));

        assertResponseFailedBecauseOfValidation(response, statusCode);
    }

    @Test
    void testInvalidOpenapiFileThrowsException() {
        assertThatExceptionOfType(OpenApiInteractionValidator.ApiLoadException.class)
                .isThrownBy(() -> new WireMockServer(getWireMockConfiguration(
                        ExtensionOptions.builder().withOpenapiFilePath(INVALID_OPENAPI_FILE_PATH))));
    }

    @Test
    void testOpenapiErrorsCanBeIgnored() {
        assertThatNoException()
                .isThrownBy(() -> new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                        .withInvalidOpenapiAllowed(true)
                        .withOpenapiFilePath(INVALID_OPENAPI_FILE_PATH))));
    }

    @Test
    void testRequestBodyMissesRequiredPropertiesWhenUsingYamlOpenapiFile() {
        WireMockServer wm = new WireMockServer(
                getWireMockConfiguration(ExtensionOptions.builder().withOpenapiFilePath(YAML_OPENAPI_FILE_PATH)));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(post(ADD_USER_URL).willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    @Test
    void testRequestWithoutContentType() {
        wm.stubFor(post(ADD_USER_URL).willReturn(created()));

        Response response = server.stubRequest(postRequestWithoutContentType(
                wm.url(ADD_USER_URL),
                JsonNodeFactory.instance
                        .objectNode()
                        .put("id", UUID.randomUUID().toString())
                        .put("username", "root")
                        .put("role", "admin")));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Request Content-Type header is missing");
    }

    @Test
    void testResponseWithoutContentType() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(JsonNodeFactory.instance
                                .arrayNode()
                                .add(JsonNodeFactory.instance
                                        .objectNode()
                                        .put("id", UUID.randomUUID().toString())
                                        .put("username", "root")
                                        .put("role", "admin"))
                                .toString())));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Response Content-Type header is missing");
    }

    @Test
    void testResponseWithoutContentTypeButWithDefaultResponseContentType() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(
                ExtensionOptions.builder().withDefaultResponseContentType(MediaType.JSON_UTF_8.toString())));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(get(GET_USERS_URL)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(JsonNodeFactory.instance
                                .arrayNode()
                                .add(JsonNodeFactory.instance
                                        .objectNode()
                                        .put("id", UUID.randomUUID().toString())
                                        .put("username", "root")
                                        .put("role", "admin"))
                                .toString())));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
        assertThat(response.getHeaders().getContentTypeHeader().values())
                .as("response content-type should match default content-type, got \"%s\"")
                .isEqualTo(List.of(MediaType.JSON_UTF_8.toString()));
    }

    @Test
    void testResponseWithContentTypeAndWithDefaultResponseContentType() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(
                ExtensionOptions.builder().withDefaultResponseContentType(MediaType.PLAIN_TEXT_UTF_8.toString())));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(get(GET_USERS_URL)
                .willReturn(okJson(JsonNodeFactory.instance
                        .arrayNode()
                        .add(JsonNodeFactory.instance
                                .objectNode()
                                .put("id", UUID.randomUUID().toString())
                                .put("username", "root")
                                .put("role", "admin"))
                        .toString())));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_OK);
        assertThat(response.getHeaders().getContentTypeHeader().values())
                .as("response content-type should match mapping content-type")
                .isEqualTo(List.of("application/json"));
    }

    protected WireMockConfiguration getDefaultWireMockConfiguration() {
        return getWireMockConfiguration(ExtensionOptions.builder());
    }

    protected WireMockConfiguration getWireMockConfiguration(final ExtensionOptions.Builder builder) {
        return wireMockConfig()
                .httpServerFactory(factory)
                .extensions(new ValidationResponseTransformer(builder.withValidatorName(validatorName)
                        .withOpenapiFilePath(Optional.ofNullable(builder.getOpenapiFilePath())
                                .orElse(JSON_OPENAPI_FILE_PATH))
                        .build()));
    }

    protected void assertResponseFailedBecauseOfValidation(final Response response) {
        assertResponseFailedBecauseOfValidation(response, DEFAULT_VALIDATION_FAILURE_STATUS_CODE);
    }

    protected void assertResponseFailedBecauseOfValidation(final Response response, final int statusCode) {
        assertAll(
                "Response failed because of OpenAPI validation",
                () -> assertThat(response.getStatus()).isEqualTo(statusCode),
                () -> assertThat(response.getHeaders().getHeader("Content-Type"))
                        .extracting(MultiValue::firstValue)
                        .isEqualTo(MediaType.HTML_UTF_8.toString()),
                () -> assertThat(response.getBodyAsString()).contains("Validation against OpenAPI failed"));
    }
}
