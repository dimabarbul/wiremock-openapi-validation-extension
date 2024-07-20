package io.github.dimabarbul.wiremock.openapi_validation;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
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
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class AtlassianOpenApiValidator implements OpenApiValidator {

    private final String openapiFilePath;
    private final boolean ignoreOpenapiErrors;
    private final OpenApiValidatorOptions options;
    private final OpenApiInteractionValidator atlassianValidator;

    AtlassianOpenApiValidator(final String openapiFilePath,
                              final boolean ignoreOpenapiErrors,
                              final OpenApiValidatorOptions options) {
        this.openapiFilePath = openapiFilePath;
        this.ignoreOpenapiErrors = ignoreOpenapiErrors;
        this.options = options;
        atlassianValidator = buildOpenApiValidator(openapiFilePath, ignoreOpenapiErrors, options);
    }

    @Override
    public OpenApiValidator withOptions(final OpenApiValidatorOptions options) {
        if (this.options.getIgnoredErrors() == options.getIgnoredErrors()) {
            return this;
        }

        return new AtlassianOpenApiValidator(openapiFilePath, ignoreOpenapiErrors, options);
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
                                                                     final boolean ignoreOpenapiErrors,
                                                                     final OpenApiValidatorOptions options) {
        final ImmutableList<String> ignoredErrors = options.getIgnoredErrors();

        final OpenApiInteractionValidator.Builder builder = createValidatorBuilder(openapiFilePath, ignoreOpenapiErrors);

        return builder
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
                                                                              final boolean ignoreOpenapiErrors) {
        return ignoreOpenapiErrors
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
}
