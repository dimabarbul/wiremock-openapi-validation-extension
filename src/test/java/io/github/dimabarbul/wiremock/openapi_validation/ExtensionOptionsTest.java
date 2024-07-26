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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

class ExtensionOptionsTest {

    @Test
    public void testFromSystemParametersNothingIsSet() {
        final SystemAccessor systemAccessor = new TestSystemAccessor.Builder()
                .build();

        ExtensionOptions options = ExtensionOptions
                .fromSystemParameters(systemAccessor);

        assertThat(options).isNotNull();
        assertAll("correct default parameters",
                () -> assertThat(options.shouldPrintConfiguration()).isFalse(),
                () -> assertThat(options.getOpenapiFilePath()).isNull(),
                () -> assertThat(options.isInvalidOpenapiAllowed()).isFalse(),
                () -> assertThat(options.getFailureStatusCode()).isEqualTo(500),
                () -> assertThat(options.getIgnoredErrors()).isEmpty(),
                () -> assertThat(options.getValidatorName()).isEqualTo("atlassian"));
    }

    @Test
    public void testFromSystemParametersEverythingIsSetInSystemProperties() {
        final SystemAccessor systemAccessor = new TestSystemAccessor.Builder()
                .addSystemProperties("openapi_validation_print_config", "true")
                .addSystemProperties("openapi_validation_file_path", "test")
                .addSystemProperties("openapi_validation_allow_invalid_openapi", "true")
                .addSystemProperties("openapi_validation_failure_status_code", "512")
                .addSystemProperties("openapi_validation_ignore_errors", "1,2, 3 ")
                .addSystemProperties("openapi_validation_validator_name", "validator")
                .build();

        ExtensionOptions options = ExtensionOptions
                .fromSystemParameters(systemAccessor);

        assertThat(options).isNotNull();
        assertAll("correct filled parameters",
                () -> assertThat(options.shouldPrintConfiguration()).isTrue(),
                () -> assertThat(options.getOpenapiFilePath()).isEqualTo("test"),
                () -> assertThat(options.isInvalidOpenapiAllowed()).isTrue(),
                () -> assertThat(options.getFailureStatusCode()).isEqualTo(512),
                () -> assertThat(options.getIgnoredErrors()).containsExactly("1", "2", "3"),
                () -> assertThat(options.getValidatorName()).isEqualTo("validator"));
    }

    @Test
    public void testFromSystemParametersEverythingIsSetInEnvironmentVariables() {
        final SystemAccessor systemAccessor = new TestSystemAccessor.Builder()
                .addEnvironmentVariables("OPENAPI_VALIDATION_PRINT_CONFIG", "true")
                .addEnvironmentVariables("OPENAPI_VALIDATION_FILE_PATH", "test")
                .addEnvironmentVariables("OPENAPI_VALIDATION_ALLOW_INVALID_OPENAPI", "true")
                .addEnvironmentVariables("OPENAPI_VALIDATION_FAILURE_STATUS_CODE", "512")
                .addEnvironmentVariables("OPENAPI_VALIDATION_IGNORE_ERRORS", "1, 2 ,3")
                .addEnvironmentVariables("OPENAPI_VALIDATION_VALIDATOR_NAME", "validator")
                .build();

        ExtensionOptions options = ExtensionOptions
                .fromSystemParameters(systemAccessor);

        assertThat(options).isNotNull();
        assertAll("correct filled parameters",
                () -> assertThat(options.shouldPrintConfiguration()).isTrue(),
                () -> assertThat(options.getOpenapiFilePath()).isEqualTo("test"),
                () -> assertThat(options.isInvalidOpenapiAllowed()).isTrue(),
                () -> assertThat(options.getFailureStatusCode()).isEqualTo(512),
                () -> assertThat(options.getIgnoredErrors()).containsExactly("1", "2", "3"),
                () -> assertThat(options.getValidatorName()).isEqualTo("validator"));
    }

    @Test
    void testMergeWithEmptyServeEvent()
            throws JsonProcessingException {
        final ExtensionOptions originalOptions = ExtensionOptions.builder()
                .withConfigurationPrinted(true)
                .withOpenapiFilePath("filePath")
                .withInvalidOpenapiAllowed(true)
                .withValidatorName("validator")
                .withFailureStatusCode(123)
                .withIgnoredErrors(List.of("error1", "error2"))
                .build();
        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(
                new ObjectMapper().readValue("{}", ServeEvent.class));

        final ExtensionOptions mergedOptions = ExtensionOptions.builder(originalOptions)
                .mergeWith(parameters)
                .build();

        assertAll("merged options are correct",
                () -> assertThat(mergedOptions.shouldPrintConfiguration()).isEqualTo(originalOptions.shouldPrintConfiguration()),
                () -> assertThat(mergedOptions.getOpenapiFilePath()).isEqualTo(originalOptions.getOpenapiFilePath()),
                () -> assertThat(mergedOptions.getIgnoredErrors()).isSameAs(originalOptions.getIgnoredErrors()),
                () -> assertThat(mergedOptions.getValidatorName()).isEqualTo(originalOptions.getValidatorName()),
                () -> assertThat(mergedOptions.isInvalidOpenapiAllowed()).isEqualTo(originalOptions.isInvalidOpenapiAllowed()),
                () -> assertThat(mergedOptions.getFailureStatusCode()).isEqualTo(originalOptions.getFailureStatusCode()));
    }

    @Test
    void testMergeWithFilledServeEvent()
            throws JsonProcessingException {
        final ExtensionOptions originalOptions = ExtensionOptions.builder()
                .withConfigurationPrinted(true)
                .withOpenapiFilePath("filePath")
                .withInvalidOpenapiAllowed(true)
                .withValidatorName("validator")
                .withFailureStatusCode(123)
                .withIgnoredErrors(List.of("error1", "error2", "error3"))
                .build();
        final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        final String json = jsonFactory.objectNode()
                .set("mapping", jsonFactory.objectNode()
                        .set("response", jsonFactory.objectNode()
                                .set("transformerParameters", jsonFactory.objectNode()
                                        .setAll(Map.<String, JsonNode>of(
                                                "openapiValidationOpenapiPrintConfig", jsonFactory.booleanNode(false),
                                                "openapiValidationOpenapiFilePath", jsonFactory.textNode("anotherFile"),
                                                "openapiValidationOpenapiIgnoreOpenapiErrors", jsonFactory.booleanNode(false),
                                                "openapiValidationOpenapiValidatorName", jsonFactory.textNode("anotherValidator"),
                                                "openapiValidationFailureStatusCode", jsonFactory.numberNode(418),
                                                "openapiValidationIgnoreErrors", jsonFactory.objectNode()
                                                        .setAll(Map.<String, JsonNode>of(
                                                                "error1", jsonFactory.booleanNode(true),
                                                                "error2", jsonFactory.booleanNode(false),
                                                                "error4", jsonFactory.booleanNode(true),
                                                                "error5", jsonFactory.booleanNode(false))))))))
                .toString();
        final ServeEvent serveEvent = new ObjectMapper().readValue(json, ServeEvent.class);
        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        final ExtensionOptions mergedOptions = ExtensionOptions.builder(originalOptions)
                .mergeWith(parameters)
                .build();

        assertAll("merged options are correct",
                () -> assertThat(mergedOptions.shouldPrintConfiguration()).isEqualTo(originalOptions.shouldPrintConfiguration()),
                () -> assertThat(mergedOptions.getOpenapiFilePath()).isEqualTo(originalOptions.getOpenapiFilePath()),
                () -> assertThat(mergedOptions.isInvalidOpenapiAllowed()).isEqualTo(originalOptions.isInvalidOpenapiAllowed()),
                () -> assertThat(mergedOptions.getValidatorName()).isEqualTo(originalOptions.getValidatorName()),
                () -> assertThat(mergedOptions.getFailureStatusCode()).isEqualTo(parameters.getFailureStatusCode()),
                () -> assertThat(mergedOptions.getIgnoredErrors()).containsExactlyInAnyOrder("error1", "error3", "error4"));
    }

    @Test
    void testMergeWithPartiallyFilledServeEvent()
            throws JsonProcessingException {
        final ExtensionOptions originalOptions = ExtensionOptions.builder()
                .withConfigurationPrinted(true)
                .withOpenapiFilePath("filePath")
                .withInvalidOpenapiAllowed(true)
                .withValidatorName("validator")
                .withFailureStatusCode(123)
                .withIgnoredErrors(List.of("error1", "error2", "error3"))
                .build();
        final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        final String json = jsonFactory.objectNode()
                .set("mapping", jsonFactory.objectNode()
                        .set("response", jsonFactory.objectNode()
                                .set("transformerParameters", jsonFactory.objectNode()
                                        .setAll(Map.<String, JsonNode>of(
                                                "openapiValidationFailureStatusCode", jsonFactory.numberNode(418))))))
                .toString();
        final ServeEvent serveEvent = new ObjectMapper().readValue(json, ServeEvent.class);
        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        final ExtensionOptions mergedOptions = ExtensionOptions.builder(originalOptions)
                .mergeWith(parameters)
                .build();

        assertAll("merged options are correct",
                () -> assertThat(mergedOptions.shouldPrintConfiguration()).isEqualTo(originalOptions.shouldPrintConfiguration()),
                () -> assertThat(mergedOptions.getOpenapiFilePath()).isEqualTo(originalOptions.getOpenapiFilePath()),
                () -> assertThat(mergedOptions.getIgnoredErrors()).isSameAs(originalOptions.getIgnoredErrors()),
                () -> assertThat(mergedOptions.getValidatorName()).isEqualTo(originalOptions.getValidatorName()),
                () -> assertThat(mergedOptions.isInvalidOpenapiAllowed()).isEqualTo(originalOptions.isInvalidOpenapiAllowed()),
                () -> assertThat(mergedOptions.getFailureStatusCode()).isEqualTo(parameters.getFailureStatusCode()));
    }

    private static class TestSystemAccessor implements SystemAccessor {
        private final Map<String, String> environmentVariables;
        private final Map<String, String> systemProperties;

        public TestSystemAccessor(final Map<String, String> environmentVariables,
                                  final Map<String, String> systemProperties) {
            this.environmentVariables = environmentVariables;
            this.systemProperties = systemProperties;
        }

        @Override
        public Optional<String> getEnvironmentVariable(final ValidationParameter parameter) {
            return Optional.ofNullable(environmentVariables.getOrDefault(parameter.envName(), null));
        }

        @Override
        public Optional<String> getSystemProperty(final ValidationParameter parameter) {
            return Optional.ofNullable(systemProperties.getOrDefault(parameter.systemPropertyName(), null));
        }

        public static class Builder {

            private final Map<String, String> environmentVariables = new HashMap<>();
            private final Map<String, String> systemProperties = new HashMap<>();

            public Builder addEnvironmentVariables(final String name, final String value) {
                this.environmentVariables.put(name, value);
                return this;
            }

            public Builder addSystemProperties(final String name, final String value) {
                this.systemProperties.put(name, value);
                return this;
            }

            public TestSystemAccessor build() {
                return new TestSystemAccessor(environmentVariables, systemProperties);
            }
        }
    }
}