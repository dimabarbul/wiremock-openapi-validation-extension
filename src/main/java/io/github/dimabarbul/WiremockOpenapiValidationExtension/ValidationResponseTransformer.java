package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
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

    private static final String DEFAULT_OPENAPI_FILE_PATH = "/var/wiremock/openapi.json";

    private final OpenApiInteractionValidator validator;

    public ValidationResponseTransformer() {
        validator = OpenApiInteractionValidator
                .createForSpecificationUrl(getOpenapiFilePath())
                .build();
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        com.atlassian.oai.validator.model.Request request = convertRequest(serveEvent.getRequest());
        ValidationReport requestReport = validator.validateRequest(request);
        ValidationReport responseReport = validator.validateResponse(request.getPath(), request.getMethod(), convertResponse(response));

        if (requestReport.hasErrors() || responseReport.hasErrors()) {
            return ErrorResponseBuilder.buildResponse(requestReport, responseReport);
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
        return Optional.ofNullable(System.getProperty("openapi_validation_filepath"))
                .orElse(
                        Optional.ofNullable(System.getenv("OPENAPI_VALIDATION_FILEPATH"))
                                .orElse(DEFAULT_OPENAPI_FILE_PATH));
    }
}
