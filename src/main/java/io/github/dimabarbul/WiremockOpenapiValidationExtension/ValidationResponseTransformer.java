package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import java.io.File;
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
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

/**
 * WireMock response transformer that validates request and response against OpenAPI file.
 */
public class ValidationResponseTransformer implements ResponseTransformerV2 {

    private static final Parameter PARAMETER_OPENAPI_FILE_PATH = new Parameter("openapi_validation_file_path");
    private static final Parameter PARAMETER_FAILURE_STATUS_CODE = new Parameter("openapi_validation_failure_status_code");
    private static final Parameter PARAMETER_IGNORE_ERRORS = new Parameter("openapi_validation_ignore_errors");

    private static final String DEFAULT_JSON_OPENAPI_FILE_PATH = "/home/wiremock/openapi.json";
    private static final String DEFAULT_YAML_OPENAPI_FILE_PATH = "/home/wiremock/openapi.yaml";
    private static final String DEFAULT_YML_OPENAPI_FILE_PATH = "/home/wiremock/openapi.yml";
    private static final int DEFAULT_FAILURE_STATUS_CODE = 500;

    private final OpenApiInteractionValidator globalValidator;
    private final int globalFailureStatusCode;
    private OpenApiInteractionValidator.Builder globalValidatorBuilder;

    public ValidationResponseTransformer() {
        globalFailureStatusCode = getGlobalFailureStatusCode();
        final Set<String> ignoredErrors = getGlobalIgnoredErrors();
        globalValidatorBuilder = OpenApiInteractionValidator.createForSpecificationUrl(getOpenapiFilePath());
        if (!ignoredErrors.isEmpty()) {
            globalValidatorBuilder = globalValidatorBuilder.withLevelResolver(LevelResolver.create()
                            .withLevels(ignoredErrors
                                    .stream()
                                    .collect(Collectors.toMap(
                                            e -> e,
                                            e -> ValidationReport.Level.IGNORE)))
                    .build());
        }
        globalValidator = globalValidatorBuilder.build();
    }

    @Override
    public Response transform(final Response response, final ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        com.atlassian.oai.validator.model.Request request = convertRequest(serveEvent.getRequest());
        Parameters transformerParameters = serveEvent.getTransformerParameters();
        Object ignoredErrors = transformerParameters.get(
                PARAMETER_IGNORE_ERRORS.transformerParameterName());
        OpenApiInteractionValidator validator = getValidatorWithAdditionalIgnoredErrors(ignoredErrors);
        ValidationReport requestReport = validator.validateRequest(request);
        ValidationReport responseReport = validator.validateResponse(
                request.getPath(), request.getMethod(), convertResponse(response));

        if (requestReport.hasErrors() || responseReport.hasErrors()) {
            int failureStatusCode = transformerParameters.getInt(
                    PARAMETER_FAILURE_STATUS_CODE.transformerParameterName(), globalFailureStatusCode);
            Response errorResponse = ErrorResponseBuilder.buildResponse(failureStatusCode, requestReport, responseReport);
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

    private static String getOpenapiFilePath() {
        return getGlobalParameter(PARAMETER_OPENAPI_FILE_PATH)
                .orElseGet(ValidationResponseTransformer::getFirstExistingOpenApiFile);
    }

    private static String getFirstExistingOpenApiFile() {
        if (new File(DEFAULT_JSON_OPENAPI_FILE_PATH).exists()) {
            return DEFAULT_JSON_OPENAPI_FILE_PATH;
        }
        if (new File(DEFAULT_YAML_OPENAPI_FILE_PATH).exists()) {
            return DEFAULT_YAML_OPENAPI_FILE_PATH;
        }

        if (new File(DEFAULT_YML_OPENAPI_FILE_PATH).exists()) {
            return DEFAULT_YML_OPENAPI_FILE_PATH;
        }

        throw new RuntimeException(String.format(
                "Cannot find OpenAPI file. Checked locations: %s, %s, %s",
                DEFAULT_JSON_OPENAPI_FILE_PATH,
                DEFAULT_YAML_OPENAPI_FILE_PATH,
                DEFAULT_YML_OPENAPI_FILE_PATH));
    }

    private static int getGlobalFailureStatusCode() {
        return getGlobalParameter(PARAMETER_FAILURE_STATUS_CODE)
                .map(Integer::parseInt)
                .orElse(DEFAULT_FAILURE_STATUS_CODE);
    }

    private static Set<String> getGlobalIgnoredErrors() {
        return getGlobalParameter(PARAMETER_IGNORE_ERRORS)
                .map(e -> Arrays.stream(e.split(",")).collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    private static Optional<String> getGlobalParameter(final Parameter parameter) {
        return Optional.ofNullable(
                Optional.ofNullable(System.getProperty(parameter.systemPropertyName()))
                    .orElseGet(() -> System.getenv(parameter.envName())));
    }

    private OpenApiInteractionValidator getValidatorWithAdditionalIgnoredErrors(final Object ignoredErrors) {
        if (ignoredErrors == null) {
            return globalValidator;
        }

        if (!(ignoredErrors instanceof Map)) {
            throw new RuntimeException("Ignored errors must be map string to boolean");
        }

        //noinspection unchecked - there is try-catch later
        Map<String, Boolean> localIgnoredErrors = (Map<String, Boolean>) ignoredErrors;

        if (localIgnoredErrors.isEmpty()) {
            return globalValidator;
        }

        Set<String> resultIgnoredErrors = getGlobalIgnoredErrors();

        try {
            resultIgnoredErrors.addAll(localIgnoredErrors.entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()));
            localIgnoredErrors.entrySet()
                    .stream()
                    .filter(e -> !e.getValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
                    .forEach(resultIgnoredErrors::remove);
        } catch (ClassCastException e) {
            throw new RuntimeException("Ignored errors must be map string to boolean");
        }

        return globalValidatorBuilder.withLevelResolver(LevelResolver.create()
                        .withLevels(resultIgnoredErrors
                                .stream().collect(Collectors.toMap(
                                        Object::toString,
                                        e -> ValidationReport.Level.IGNORE)))
                        .build())
                .build();
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
