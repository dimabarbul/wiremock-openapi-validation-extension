package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.dimabarbul.WiremockOpenapiValidationExtension.RequestBuilder.deleteRequest;
import static io.github.dimabarbul.WiremockOpenapiValidationExtension.RequestBuilder.getRequest;
import static io.github.dimabarbul.WiremockOpenapiValidationExtension.RequestBuilder.postJsonRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

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

abstract class ValidationResponseTransformerTest {

    private static final int DEFAULT_VALIDATION_FAILURE_STATUS_CODE = 500;
    private static final String GET_USERS_URL = "/users";
    private static final String ADD_USER_URL = "/users";
    private static final String DELETE_USER_URL = "/users/123";
    private static final String JSON_OPENAPI_FILE_PATH = "src/test/resources/openapi.json";
    private static final String YAML_OPENAPI_FILE_PATH = "src/test/resources/openapi.yaml";
    private static final String INVALID_OPENAPI_FILE_PATH = "src/test/resources/invalid_openapi.json";

    private final DirectCallHttpServerFactory factory;
    private final DirectCallHttpServer server;

    private final String validatorName;
    private final WireMockServer wm;

    public ValidationResponseTransformerTest(final String validatorName) {
        this.validatorName = validatorName;
        factory = new DirectCallHttpServerFactory();
        wm = new WireMockServer(getDefaultWireMockConfiguration());
        server = factory.getHttpServer();
    }

    @Test
    void testRequestBodyHasRequiredProperties() {
        wm.stubFor(post(ADD_USER_URL)
                .willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(
                wm.url(ADD_USER_URL),
                JsonNodeFactory.instance.objectNode()
                        .put("id", UUID.randomUUID().toString())
                        .put("username", "root")
                        .put("role", "admin")));

        assertThat(response.getStatus())
                .as("response should be successful, got body \"%s\"", response.getBodyAsString())
                .isEqualTo(HttpStatus.SC_CREATED);
    }

    @Test
    void testRequestBodyMissesRequiredProperties() {
        wm.stubFor(post(ADD_USER_URL)
                .willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    @Test
    void testRequestBodyHasInvalidProperties() {
        wm.stubFor(post(ADD_USER_URL)
                .willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(
                wm.url(ADD_USER_URL),
                JsonNodeFactory.instance.objectNode()
                        .put("id", "test")
                        .put("username", "toolongusername")
                        .put("name", "x")
                        .put("dob", "invalid date string")
                        .put("role", "unknown")
                        .put("extra", "extra")));

        assertResponseFailedBecauseOfValidation(response);
        assertAll(
                "Request validation errors",
                () -> assertThat(response.getBodyAsString()).contains("[Path '/id'] Input string \"test\" is not a valid UUID"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/username'] String \"toolongusername\" is too long (length: 15, maximum allowed: 10)"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/name'] String \"x\" is too short (length: 1, required minimum: 2)"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/dob'] String \"invalid date string\" is invalid against requested date format(s) yyyy-MM-dd"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/role'] Instance value (\"unknown\") not found in enum (possible values: [\"user\",\"admin\"])"),
                () -> assertThat(response.getBodyAsString()).contains("Object instance has properties which are not allowed by the schema: [\"extra\"]"));
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
        wm.stubFor(delete(urlPathEqualTo(DELETE_USER_URL))
                .willReturn(noContent()));

        Response response = server.stubRequest(deleteRequest(wm.url(DELETE_USER_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Query parameter 'soft' is required on path '/users/{userId}' but not found in request.");
    }

    @Test
    void testResponseHasRequiredProperties() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse(
                        JsonNodeFactory.instance.arrayNode()
                                .add(JsonNodeFactory.instance.objectNode()
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
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse("[{}]", HttpStatus.SC_OK)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    @Test
    void testResponseHasInvalidProperties() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse(
                        JsonNodeFactory.instance.arrayNode()
                                .add(JsonNodeFactory.instance.objectNode()
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
                () -> assertThat(response.getBodyAsString()).contains("[Path '/0/id'] Input string \"test\" is not a valid UUID"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/0/username'] String \"toolongusername\" is too long (length: 15, maximum allowed: 10)"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/0/name'] String \"x\" is too short (length: 1, required minimum: 2)"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/0/dob'] String \"invalid date string\" is invalid against requested date format(s) yyyy-MM-dd"),
                () -> assertThat(response.getBodyAsString()).contains("[Path '/0/role'] Instance value (\"unknown\") not found in enum (possible values: [\"user\",\"admin\"])"));
    }

    @Test
    void testResponseHasInvalidStatusCode() {
        wm.stubFor(get(GET_USERS_URL)
                .willReturn(jsonResponse("", HttpStatus.SC_NOT_FOUND)));

        Response response = server.stubRequest(getRequest(wm.url(GET_USERS_URL)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Response status 404 not defined for path '" + GET_USERS_URL + "'.");
    }

    @Test
    void testUnknownPath() {
        wm.stubFor(get(UrlPattern.ANY)
                .willReturn(noContent()));

        String unknownPath = "/unknown";
        Response response = server.stubRequest(getRequest(wm.url(unknownPath)));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("No API path found that matches request '" + unknownPath + "'.");
    }

    @Test
    void testUnmatchedResponse() {
        wm.stubFor(get("/test")
                .willReturn(noContent()));

        Response response = server.stubRequest(getRequest(wm.url("/not-test")));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void testGlobalCustomResponseCodeOnValidationFailure() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                .withFailureStatusCode(599)));

        wm.stubFor(get(UrlPattern.ANY)
                .willReturn(noContent()));

        DirectCallHttpServer server = factory.getHttpServer();

        Response response = server.stubRequest(getRequest(wm.url("/test")));

        assertResponseFailedBecauseOfValidation(response, 599);
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
    void testCustomResponseCodeOnValidationFailure() {
        int statusCode = 598;
        wm.stubFor(get(UrlPattern.ANY)
                .willReturn(noContent()
                        .withTransformerParameter("openapiValidationFailureStatusCode", statusCode)));

        Response response = server.stubRequest(getRequest(wm.url("/test")));

        assertResponseFailedBecauseOfValidation(response, statusCode);
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

    @Test
    void testInvalidOpenapiFileThrowsException() {
        assertThatExceptionOfType(OpenApiInteractionValidator.ApiLoadException.class)
                .isThrownBy(() ->
                        new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                                .withOpenapiFilePath(INVALID_OPENAPI_FILE_PATH))));
    }

    @Test
    void testInvalidOpenapiErrorsCanBeIgnored() {
        assertThatNoException()
                .isThrownBy(() -> new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                        .ignoreOpenapiErrors(true)
                        .withOpenapiFilePath(INVALID_OPENAPI_FILE_PATH))));
    }

    @Test
    void testRequestBodyMissesRequiredPropertiesWhenUsingYamlOpenapiFile() {
        WireMockServer wm = new WireMockServer(getWireMockConfiguration(ExtensionOptions.builder()
                .withOpenapiFilePath(YAML_OPENAPI_FILE_PATH)));
        DirectCallHttpServer server = factory.getHttpServer();

        wm.stubFor(post(ADD_USER_URL)
                .willReturn(created()));

        Response response = server.stubRequest(postJsonRequest(wm.url(ADD_USER_URL), "{}"));

        assertResponseFailedBecauseOfValidation(response);
        assertThat(response.getBodyAsString()).contains("Object has missing required properties");
    }

    private WireMockConfiguration getDefaultWireMockConfiguration() {
        return getWireMockConfiguration(ExtensionOptions.builder());
    }

    private WireMockConfiguration getWireMockConfiguration(final ExtensionOptions.Builder builder) {
        return wireMockConfig()
                .httpServerFactory(factory)
                .extensions(new ValidationResponseTransformer(builder
                        .withValidatorName(validatorName)
                        .withOpenapiFilePath(Optional.ofNullable(builder.getOpenapiFilePath()).orElse(JSON_OPENAPI_FILE_PATH))
                        .build()));
    }

    private void assertResponseFailedBecauseOfValidation(final Response response) {
        assertResponseFailedBecauseOfValidation(response, DEFAULT_VALIDATION_FAILURE_STATUS_CODE);
    }

    private void assertResponseFailedBecauseOfValidation(final Response response, final int statusCode) {
        assertAll(
                "Response failed because of OpenAPI validation",
                () -> assertThat(response.getStatus()).isEqualTo(statusCode),
                () -> assertThat(response.getHeaders().getHeader("Content-Type"))
                        .extracting(MultiValue::firstValue)
                        .isEqualTo(MediaType.HTML_UTF_8.toString()),
                () -> assertThat(response.getBodyAsString()).contains("Validation against OpenAPI failed"));
    }
}
