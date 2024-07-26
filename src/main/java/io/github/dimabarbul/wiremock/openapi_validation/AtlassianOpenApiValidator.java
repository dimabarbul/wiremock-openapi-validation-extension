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

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.interaction.request.CustomRequestValidator;
import com.atlassian.oai.validator.interaction.response.CustomResponseValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableList;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class AtlassianOpenApiValidator implements OpenApiValidator {

    private final String openapiFilePath;
    private final boolean allowInvalidOpenapi;
    private final OpenApiValidatorOptions options;
    private final OpenApiInteractionValidator atlassianValidator;

    AtlassianOpenApiValidator(final String openapiFilePath,
                              final boolean allowInvalidOpenapi,
                              final OpenApiValidatorOptions options) {
        this.openapiFilePath = openapiFilePath;
        this.allowInvalidOpenapi = allowInvalidOpenapi;
        this.options = options;
        atlassianValidator = buildOpenApiValidator(openapiFilePath, allowInvalidOpenapi, options);
    }

    @Override
    public OpenApiValidator withOptions(final OpenApiValidatorOptions options) {
        if (this.options.getIgnoredErrors() == options.getIgnoredErrors()) {
            return this;
        }

        return new AtlassianOpenApiValidator(openapiFilePath, allowInvalidOpenapi, options);
    }

    @Override
    public ValidationResult validateRequest(final LoggedRequest request) {
        final com.atlassian.oai.validator.model.Request atlassianRequest = convertRequest(request);
        final ValidationReport requestReport = atlassianValidator.validateRequest(atlassianRequest);
        return createValidationResult(requestReport);
    }

    @Override
    public ValidationResult validateResponse(final LoggedRequest request, final Response response) {
        final com.atlassian.oai.validator.model.Request atlassianRequest = convertRequest(request);
        final com.atlassian.oai.validator.model.Response convertedResponse = convertResponse(response);
        final ValidationReport responseReport = atlassianValidator.validateResponse(
                atlassianRequest.getPath(), atlassianRequest.getMethod(), convertedResponse);
        return createValidationResult(responseReport);
    }

    private static OpenApiInteractionValidator buildOpenApiValidator(final String openapiFilePath,
                                                                     final boolean allowInvalidOpenapi,
                                                                     final OpenApiValidatorOptions options) {
        final ImmutableList<String> ignoredErrors = options.getIgnoredErrors();

        final OpenApiInteractionValidator.Builder builder = createValidatorBuilder(openapiFilePath, allowInvalidOpenapi);

        return builder
                .withCustomRequestValidation(new RequireContentTypeRequestValidator())
                .withCustomResponseValidation(new RequireContentTypeResponseValidator())
                .withLevelResolver(LevelResolver.create()
                        .withLevels(ignoredErrors
                                .stream()
                                .collect(Collectors.toMap(
                                        e -> e,
                                        e -> ValidationReport.Level.IGNORE)))
                        .build())
                .build();
    }

    private static OpenApiInteractionValidator.Builder createValidatorBuilder(final String openapiFilePath,
                                                                              final boolean allowInvalidOpenapi) {
        return allowInvalidOpenapi
                ? createValidatorBuilderIgnoringOpenapiErrors(openapiFilePath)
                : OpenApiInteractionValidator.createForSpecificationUrl(openapiFilePath);
    }

    private static OpenApiInteractionValidator.Builder createValidatorBuilderIgnoringOpenapiErrors(final String openapiFilePath) {
        final SwaggerParseResult swaggerParseResult = new OpenAPIParser().readLocation(openapiFilePath, null, defaultParseOptions());
        if (swaggerParseResult.getOpenAPI() == null) {
            throw new OpenApiInteractionValidator.ApiLoadException(openapiFilePath, swaggerParseResult);
        }

        return OpenApiInteractionValidator.createFor(swaggerParseResult.getOpenAPI());
    }

    private static ParseOptions defaultParseOptions() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        return parseOptions;
    }

    private static com.atlassian.oai.validator.model.Request convertRequest(final Request request) {
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(
                request.getMethod().toString(), request.getUrl());

        final Map<String, QueryParameter> queryParameters = Urls.splitQuery(URI.create(request.getUrl()));
        queryParameters.forEach((k, v) -> builder.withQueryParam(v.key(), v.values()));

        for (final String key : request.getAllHeaderKeys()) {
            builder.withHeader(key, request.getHeader(key));
        }

        return builder
                .withBody(request.getBody())
                .build();
    }

    private static com.atlassian.oai.validator.model.Response convertResponse(final Response response) {
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(response.getStatus())
                .withBody(response.getBody());
        response.getHeaders().all().forEach((header) -> builder.withHeader(header.key(), header.values()));
        return builder.build();
    }

    private static ValidationResult createValidationResult(final ValidationReport report) {
        final ValidationResult.Builder builder = ValidationResult.builder();

        report.getMessages()
                .stream().filter(m -> m.getLevel() == ValidationReport.Level.ERROR)
                .forEach(m -> builder.addError(m.getKey(), m.getMessage()));

        return builder.build();
    }

    private static class RequireContentTypeRequestValidator implements CustomRequestValidator {

        public static final String VALIDATION_REQUEST_CONTENT_TYPE_MISSING_KEY = "validation.request.contentType.missing";
        public static final String VALIDATION_REQUEST_CONTENT_TYPE_MISSING_MESSAGE = "Request Content-Type header is missing";

        @Override
        public ValidationReport validate(@Nonnull final com.atlassian.oai.validator.model.Request request,
                                         @Nonnull final ApiOperation apiOperation) {
            final RequestBody requestBodySchema = apiOperation.getOperation().getRequestBody();
            if (requestBodySchema == null) {
                return ValidationReport.empty();
            }

            final Content contentSchema = requestBodySchema.getContent();
            final boolean isContentRequired = Boolean.TRUE.equals(requestBodySchema.getRequired());

            if (isContentRequired && contentSchema != null && !contentSchema.isEmpty()) {
                if (request.getContentType().isEmpty()) {
                    return ValidationReport.singleton(
                            ValidationReport.Message.create(
                                            VALIDATION_REQUEST_CONTENT_TYPE_MISSING_KEY,
                                            VALIDATION_REQUEST_CONTENT_TYPE_MISSING_MESSAGE
                                    )
                                    .build());
                }
            }

            return ValidationReport.empty();
        }
    }

    private static class RequireContentTypeResponseValidator implements CustomResponseValidator {

        public static final String VALIDATION_RESPONSE_CONTENT_TYPE_MISSING_KEY = "validation.response.contentType.missing";
        public static final String VALIDATION_RESPONSE_CONTENT_TYPE_MISSING_MESSAGE = "Response Content-Type header is missing";

        @Override
        public ValidationReport validate(@Nonnull final com.atlassian.oai.validator.model.Response response,
                                         @Nonnull final ApiOperation apiOperation) {
            final ApiResponses responses = apiOperation.getOperation().getResponses();
            final String statusString = String.valueOf(response.getStatus());
            if (responses == null || !responses.containsKey(statusString)) {
                return ValidationReport.empty();
            }

            final ApiResponse apiResponse = responses.get(statusString);
            final Content contentSchema = apiResponse.getContent();

            if (contentSchema != null && !contentSchema.isEmpty()) {
                if (response.getContentType().isEmpty()) {
                    return ValidationReport.singleton(
                            ValidationReport.Message.create(
                                            VALIDATION_RESPONSE_CONTENT_TYPE_MISSING_KEY,
                                            VALIDATION_RESPONSE_CONTENT_TYPE_MISSING_MESSAGE
                                    )
                                    .build());
                }
            }

            return ValidationReport.empty();
        }
    }
}
