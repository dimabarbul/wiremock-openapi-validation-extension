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

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ParameterizedClass
@ValueSource(strings = {"snapshot", "snapshot-alpine"})
class OpenapiValidationIT {

    static final int FAILURE_STATUS_CODE = 599;

    static GenericContainer<?> appContainer;

    static String baseUrl;

    // Required for injection imageTag into before/after invocation callbacks.
    @Parameter
    String imageTag;

    @BeforeParameterizedClassInvocation
    static void beforeInvocation(String imageTag) {
        appContainer = new GenericContainer<>("dimabarbul/wiremock-openapi-validation:" + imageTag)
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/__admin/health").forStatusCode(HttpStatus.SC_OK))
                .withEnv("OPENAPI_VALIDATION_FAILURE_STATUS_CODE", String.valueOf(FAILURE_STATUS_CODE))
                .withEnv("OPENAPI_VALIDATION_IGNORE_ERRORS", "validation.response.body.unexpected")
                .withFileSystemBind(
                        "src/test/resources/openapi.yaml", "/home/wiremock/openapi.yaml", BindMode.READ_ONLY)
                .withFileSystemBind(
                        "src/test/resources/mappings.json",
                        "/home/wiremock/mappings/mappings.json",
                        BindMode.READ_ONLY);
        appContainer.start();

        String host = appContainer.getHost();
        int port = appContainer.getMappedPort(8080);
        baseUrl = "http://" + host + ":" + port;
        WireMock.configureFor(host, port);
    }

    @AfterParameterizedClassInvocation
    public static void afterAll(final String imageTag) {
        System.out.println("Container logs (tag " + imageTag + "):");
        System.out.println(appContainer.getLogs());
        appContainer.stop();
    }

    @Test
    public void testGetUsersWorks() {
        RestAssured.get(baseUrl + "/users").then().statusCode(HttpStatus.SC_OK).body("$.size()", Matchers.equalTo(2));
    }

    @Test
    public void testDeleteUserValidMockWorksOk() {
        RestAssured.delete(baseUrl + "/users/1?soft=true")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .body(Matchers.emptyString());
    }

    @Test
    public void testDeleteUserInvalidStatusReturnsError() {
        RestAssured.delete(baseUrl + "/users/2?soft=false")
                .then()
                .statusCode(FAILURE_STATUS_CODE)
                .contentType(ContentType.HTML)
                .body(Matchers.containsString("validation.response.status.unknown"));
    }

    @Test
    public void testDeleteUserUnexpectedResponseBodyButTheErrorIgnoredOk() {
        RestAssured.delete(baseUrl + "/users/3?soft=true")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .contentType(ContentType.JSON);
    }

    @Test
    public void testDeleteUserMissingMappingReturnsWiremockError() {
        RestAssured.delete(baseUrl + "/users/4?soft=false")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.TEXT);
    }
}
