package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableList;

/**
 * WireMock response transformer that validates request and response against OpenAPI file.
 * It returns original response if request and response are valid, otherwise it replaces
 * response with one describing what exactly went wrong.
 */
public class ValidationResponseTransformer implements ResponseTransformerV2 {

    private static final List<String> DEFAULT_OPENAPI_FILE_PATHS = List.of(
            "/home/wiremock/openapi.json",
            "/home/wiremock/openapi.yaml",
            "/home/wiremock/openapi.yml");

    private final ValidationResponseTransformerOptions options;
    private final OpenApiInteractionValidator globalValidator;

    public ValidationResponseTransformer() {
        this(getDefaultOptions());
    }

    public ValidationResponseTransformer(final ValidationResponseTransformerOptions options) {
        this.options = options;
        globalValidator = buildOpenApiValidator(options);
    }

    @Override
    public Response transform(final Response response, final ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        com.atlassian.oai.validator.model.Request request = convertRequest(serveEvent.getRequest());
        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);
        final ValidationResponseTransformerOptions mergedOptions =
                ValidationResponseTransformerOptions.builder(options)
                        .mergeWith(parameters)
                        .build();
        OpenApiInteractionValidator validator = getStubSpecificValidator(mergedOptions);
        ValidationReport requestReport = validator.validateRequest(request);
        ValidationReport responseReport = validator.validateResponse(
                request.getPath(), request.getMethod(), convertResponse(response));

        if (requestReport.hasErrors() || responseReport.hasErrors()) {
            Response errorResponse = ErrorResponseBuilder.buildResponse(
                    mergedOptions.getFailureStatusCode(), requestReport, responseReport);
            log(serveEvent.getRequest(), response, errorResponse);
            return errorResponse;
        }

        return response;
    }

    @Override
    public String getName() {
        return "openapi-validation";
    }

    private static com.atlassian.oai.validator.model.Request convertRequest(final Request request) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(
                request.getMethod().toString(), request.getUrl());

        Map<String, QueryParameter> queryParameters = Urls.splitQuery(URI.create(request.getUrl()));
        queryParameters.forEach((k, v) -> builder.withQueryParam(v.key(), v.values()));

        for (String key : request.getAllHeaderKeys()) {
            builder.withHeader(key, request.getHeader(key));
        }

        return builder
                .withBody(request.getBody())
                .build();
    }

    private static com.atlassian.oai.validator.model.Response convertResponse(final Response response) {
        SimpleResponse.Builder builder = new SimpleResponse.Builder(response.getStatus())
                .withBody(response.getBody());
        response.getHeaders().all().forEach((header) -> builder.withHeader(header.key(), header.values()));
        return builder.build();
    }

    private static ValidationResponseTransformerOptions getDefaultOptions() {
        ValidationResponseTransformerOptions options = ValidationResponseTransformerOptions.fromSystemParameters();
        if (options.getOpenapiFilePath() == null) {
            options = ValidationResponseTransformerOptions.builder(options)
                    .withOpenapiFilePath(getFirstExistingFile())
                    .build();
        }
        return options;
    }

    private static String getFirstExistingFile() {
        for (final String filePath : DEFAULT_OPENAPI_FILE_PATHS) {
            if (new File(filePath).exists()) {
                return filePath;
            }
        }

        throw new RuntimeException(String.format(
                "Cannot find OpenAPI file. Checked locations: %s",
                String.join(", ", DEFAULT_OPENAPI_FILE_PATHS)));
    }

    private static OpenApiInteractionValidator buildOpenApiValidator(final ValidationResponseTransformerOptions options) {

        final OpenApiInteractionValidator.Builder builder = OpenApiInteractionValidator.createForSpecificationUrl(options.getOpenapiFilePath());
        final ImmutableList<String> ignoredErrors = options.getIgnoredErrors();
        builder.withLevelResolver(LevelResolver.create()
                .withLevels(ignoredErrors
                        .stream()
                        .collect(Collectors.toMap(
                                e -> e,
                                e -> ValidationReport.Level.IGNORE)))
                .build());
        return builder.build();
    }

    private OpenApiInteractionValidator getStubSpecificValidator(final ValidationResponseTransformerOptions specificOptions) {
        if (specificOptions.getIgnoredErrors() == options.getIgnoredErrors()) {
            return globalValidator;
        }

        return buildOpenApiValidator(specificOptions);
    }

    private static void log(final LoggedRequest request, final Response response, final Response errorResponse) {
        notifier().error(String.format(
                "OpenAPI validation error\n\n** Request **:\n%s\n\n** Response **:\n%s\n\n** Validation response **:\n%s",
                prettifyForOutput(request),
                prettifyForOutput(response),
                prettifyForOutput(errorResponse)));
    }

    private static String prettifyForOutput(final LoggedRequest request) {
        return String.format(
                "URL: %s\nHeaders:\n%s\nBody: %s",
                request.getUrl(),
                request.getHeaders(),
                request.getBodyAsString()
        );
    }

    private static String prettifyForOutput(final Response response) {
        return String.format(
                "Status code: %d\nHeaders:\n%s\nBody: %s",
                response.getStatus(),
                response.getHeaders(),
                response.getBodyAsString()
        );
    }
}
