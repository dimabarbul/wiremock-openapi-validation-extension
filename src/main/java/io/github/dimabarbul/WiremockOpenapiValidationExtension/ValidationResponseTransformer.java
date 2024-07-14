package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

/**
 * WireMock response transformer that validates request and response against OpenAPI file.
 */
public class ValidationResponseTransformer implements ResponseTransformerV2 {

    private static final String PARAMETER_OPENAPI_FILEPATH = "openapi_validation_filepath";
    public static final String PARAMETER_FAILURE_STATUS_CODE = "openapi_validation_failure_status_code";
    public static final String PARAMETER_IGNORE_ERRORS = "openapi_validation_ignore_errors";
    public static final String PARAMETER_VALIDATE_REQUEST = "openapi_validation_validate_request";
    public static final String PARAMETER_VALIDATE_RESPONSE = "openapi_validation_validate_response";

    private static final String DEFAULT_OPENAPI_FILE_PATH = "/var/wiremock/openapi.json";
    private static final int DEFAULT_FAILURE_STATUS_CODE = 500;

    public static final ValidationReport EMPTY_VALIDATION_REPORT = ValidationReport.empty();

    private final OpenApiInteractionValidator validator;
    private final int failureStatusCode;
    private final boolean validateRequest;
    private final boolean validateResponse;

    public ValidationResponseTransformer() {
        failureStatusCode = getFailureStatusCode();
        Set<String> ignoredErrors = getIgnoredErrors();
        OpenApiInteractionValidator.Builder validatorBuilder = OpenApiInteractionValidator
                .createForSpecificationUrl(getOpenapiFilePath());
        if (!ignoredErrors.isEmpty()) {
            validatorBuilder = validatorBuilder.withLevelResolver(LevelResolver.create()
                            .withLevels(ignoredErrors
                                    .stream()
                                    .collect(Collectors.toMap(
                                            e -> e,
                                            e -> ValidationReport.Level.INFO)))
                    .build());
        }
        validator = validatorBuilder.build();
        validateRequest = getValidateRequest();
        validateResponse = getValidateResponse();
    }

    @Override
    public Response transform(final Response response, final ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        com.atlassian.oai.validator.model.Request request = convertRequest(serveEvent.getRequest());
        ValidationReport requestReport = validateRequest ?
                validator.validateRequest(request) :
                EMPTY_VALIDATION_REPORT;
        ValidationReport responseReport = validateResponse ?
                validator.validateResponse(
                        request.getPath(), request.getMethod(), convertResponse(response)) :
                EMPTY_VALIDATION_REPORT;

        if (requestReport.hasErrors() || responseReport.hasErrors()) {
            return ErrorResponseBuilder.buildResponse(failureStatusCode, requestReport, responseReport);
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

    private static String getOpenapiFilePath() {
        return getParameter(PARAMETER_OPENAPI_FILEPATH)
                .orElse(DEFAULT_OPENAPI_FILE_PATH);
    }

    private static int getFailureStatusCode() {
        return getParameter(PARAMETER_FAILURE_STATUS_CODE)
                .map(Integer::parseInt)
                .orElse(DEFAULT_FAILURE_STATUS_CODE);
    }

    private static Set<String> getIgnoredErrors() {
        return getParameter(PARAMETER_IGNORE_ERRORS)
                .map(e -> Arrays.stream(e.split(",")).collect(Collectors.toSet()))
                .orElse(new HashSet<>());
    }

    private static boolean getValidateRequest() {
        return getParameter(PARAMETER_VALIDATE_REQUEST)
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    private static boolean getValidateResponse() {
        return getParameter(PARAMETER_VALIDATE_RESPONSE)
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    private static Optional<String> getParameter(final String name) {
        return Optional.ofNullable(
                Optional.ofNullable(System.getProperty(name.toLowerCase()))
                    .orElse(System.getenv(name.toUpperCase())));
    }
}
