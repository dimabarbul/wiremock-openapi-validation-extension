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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * Options for the extension, used by different classes throughout the code.
 * Use {@link ExtensionOptions#builder} or {@link ExtensionOptions#fromSystemParameters()} to construct.
 */
public final class ExtensionOptions {

    private static final int DEFAULT_FAILURE_STATUS_CODE = 500;

    private final String openapiFilePath;
    private final boolean allowInvalidOpenapi;
    private final String validatorName;
    private final int failureStatusCode;
    private final ImmutableList<String> ignoredErrors;

    private ExtensionOptions(final String openapiFilePath,
                             final boolean allowInvalidOpenapi,
                             final String validatorName,
                             final int failureStatusCode,
                             final ImmutableList<String> ignoredErrors) {
        this.openapiFilePath = openapiFilePath;
        this.allowInvalidOpenapi = allowInvalidOpenapi;
        this.validatorName = validatorName;
        this.failureStatusCode = failureStatusCode;
        this.ignoredErrors = requireNonNull(ignoredErrors);
    }

    /**
     * Create new builder.
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create new builder prepopulated with the options.
     * @param options Options to copy from
     * @return Builder prepopulated with data from the options
     */
    public static Builder builder(final ExtensionOptions options) {
        return new Builder(options);
    }

    /**
     * Create extension options reading values from environment variables and system parameters.
     * @return Extension options
     */
    public static ExtensionOptions fromSystemParameters() {
        return fromSystemParameters(SystemAccessor.INSTANCE);
    }

    static ExtensionOptions fromSystemParameters(
            final SystemAccessor systemAccessor) {
        final Builder builder = builder();
        getGlobalParameter(systemAccessor, ValidationParameter.OPENAPI_FILE_PATH).ifPresent(builder::withOpenapiFilePath);
        getGlobalParameter(systemAccessor, ValidationParameter.VALIDATOR_NAME).ifPresent(builder::withValidatorName);
        getGlobalParameter(systemAccessor, ValidationParameter.ALLOW_INVALID_OPENAPI)
                .map(Boolean::parseBoolean)
                .ifPresent(builder::withInvalidOpenapiAllowed);
        getGlobalParameter(systemAccessor, ValidationParameter.FAILURE_STATUS_CODE)
                .map(Integer::parseInt)
                .ifPresent(builder::withFailureStatusCode);
        getGlobalParameter(systemAccessor, ValidationParameter.IGNORE_ERRORS)
                .map(e -> Arrays.stream(e.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .ifPresent(builder::withIgnoredErrors);
        return builder.build();
    }

    private static Optional<String> getGlobalParameter(final SystemAccessor systemAccessor,
                                                       final ValidationParameter parameter) {
        return systemAccessor.getSystemProperty(parameter)
                .or(() -> systemAccessor.getEnvironmentVariable(parameter));
    }

    /**
     * Get OpenAPI file path.
     * @return OpenAPI file path
     */
    public String getOpenapiFilePath() {
        return openapiFilePath;
    }

    /**
     * Check whether OpenAPI errors should be tolerated or exception should be thrown
     * in case of incorrect OpenAPI file.
     * @return True to allow OpenAPI file with errors, false to throw exception
     */
    public boolean isInvalidOpenapiAllowed() {
        return allowInvalidOpenapi;
    }

    /**
     * Get HTTP status code to return in case of validation failure.
     * @return HTTP status code to return in case of validation failure
     */
    public int getFailureStatusCode() {
        return failureStatusCode;
    }

    /**
     * Get list of ignored validation errors. The errors are specific to validator being used.
     * @return List of validation errors to ignore
     */
    public ImmutableList<String> getIgnoredErrors() {
        return ignoredErrors;
    }

    /**
     * Get name of validator to use.
     * @return Name of validator to use
     */
    public String getValidatorName() {
        return validatorName;
    }

    /**
     * Builder for extension options.
     */
    public static final class Builder {

        private String openapiFilePath = null;
        private boolean allowInvalidOpenapi = false;
        private String validatorName = "atlassian";
        private int failureStatusCode = DEFAULT_FAILURE_STATUS_CODE;
        private List<String> ignoredErrors = List.of();

        /**
         * Create new builder with default values.
         */
        public Builder() {
        }

        /**
         * Create new builder copying all values from provided options.
         * @param options Options to copy values from
         */
        public Builder(final ExtensionOptions options) {
            openapiFilePath = options.getOpenapiFilePath();
            validatorName = options.getValidatorName();
            allowInvalidOpenapi = options.isInvalidOpenapiAllowed();
            failureStatusCode = options.getFailureStatusCode();
            ignoredErrors = options.getIgnoredErrors();
        }

        /**
         * Set OpenAPI file path.
         * @param openapiFilePath OpenAPI file path
         * @return Builder
         */
        public Builder withOpenapiFilePath(final String openapiFilePath) {
            this.openapiFilePath = openapiFilePath;
            return this;
        }

        /**
         * Set HTTP status code to return on validation failure.
         * @param failureStatusCode HTTP status code to return on validation failure
         * @return Builder
         */
        public Builder withFailureStatusCode(final int failureStatusCode) {
            this.failureStatusCode = failureStatusCode;
            return this;
        }

        /**
         * Set validation errors to ignore.
         * @param ignoredErrors Validation errors to ignore
         * @return Builder
         */
        public Builder withIgnoredErrors(final List<String> ignoredErrors) {
            this.ignoredErrors = ignoredErrors;
            return this;
        }

        /**
         * Set name of validator to use.
         * @param validatorName Name of validator to use
         * @return Builder
         */
        public Builder withValidatorName(final String validatorName) {
            this.validatorName = validatorName;
            return this;
        }

        /**
         * Set whether OpenAPI errors should be tolerated or exception should be thrown
         * in case of incorrect OpenAPI file.
         * @param allowInvalidOpenapi True to allow OpenAPI file with errors, false to throw exception
         * @return Builder
         */
        public Builder withInvalidOpenapiAllowed(final boolean allowInvalidOpenapi) {
            this.allowInvalidOpenapi = allowInvalidOpenapi;
            return this;
        }

        /**
         * Get OpenAPI file path.
         * @return OpenAPI file path
         */
        public String getOpenapiFilePath() {
            return openapiFilePath;
        }

        /**
         * Build extension options with values from the builder.
         * @return Extension options
         */
        public ExtensionOptions build() {
            return new ExtensionOptions(
                    openapiFilePath, allowInvalidOpenapi, validatorName, failureStatusCode, ImmutableList.copyOf(ignoredErrors));
        }

        Builder mergeWith(final ValidationTransformerParameters parameters) {
            mergeFailureStatusCode(parameters);
            mergeIgnoredErrors(parameters);

            return this;
        }

        private void mergeFailureStatusCode(final ValidationTransformerParameters parameters) {
            Optional.ofNullable(parameters.getFailureStatusCode()).ifPresent(this::withFailureStatusCode);
        }

        private void mergeIgnoredErrors(final ValidationTransformerParameters parameters) {
            final Map<String, Boolean> ignoredErrorsFromParameters = parameters.getIgnoredErrors();

            if (!ignoredErrorsFromParameters.isEmpty()) {
                final Set<String> resultIgnoredErrors = new HashSet<>(ignoredErrors);
                ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .forEach(resultIgnoredErrors::add);
                ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(e -> !e.getValue())
                        .map(Map.Entry::getKey)
                        .forEach(resultIgnoredErrors::remove);

                ignoredErrors = List.copyOf(resultIgnoredErrors);
            }
        }
    }
}
