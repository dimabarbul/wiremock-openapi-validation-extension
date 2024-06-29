package com.github.dimabarbul.WiremockOpenapiValidationExtension;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilterV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public final class ValidationRequestFilter implements StubRequestFilterV2 {

    private static final String DEFAULT_OPENAPI_FILE_PATH = "/var/wiremock/openapi.json";
    private static final int REQUEST_VALIDATION_FAILED_STATUS_CODE = 500;
    private static final String REQUEST_VALIDATION_FAILED_TEMPLATE = "Request validation failed. Errors: %s";

    @Override
    public String getName() {
        return "openapi-validation";
    }

    @Override
    public RequestFilterAction filter(Request request, ServeEvent serveEvent) {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForSpecificationUrl(getOpenapiFilePath())
                .build();
        ValidationReport report = validator.validateRequest(convertRequest(request));

        if (report.hasErrors()) {
            return RequestFilterAction.stopWith(createResponseDefinitionFor(report));
        }

        return RequestFilterAction.continueWith(request);
    }

    private static String getOpenapiFilePath() {
        return System.getProperty("openapi_validation_filepath", DEFAULT_OPENAPI_FILE_PATH);
    }

    private static com.atlassian.oai.validator.model.Request convertRequest(final Request request) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(
                request.getMethod().toString(), request.getUrl());

        for (String key : request.getAllHeaderKeys()) {
            builder.withHeader(key, request.getHeader(key));
        }

        return builder
                .withBody(request.getBody())
                .build();
    }

    private static ResponseDefinition createResponseDefinitionFor(ValidationReport report) {
        return new ResponseDefinition(REQUEST_VALIDATION_FAILED_STATUS_CODE, String.format(REQUEST_VALIDATION_FAILED_TEMPLATE, getValidationErrorMessage(report)));
    }

    private static String getValidationErrorMessage(ValidationReport report) {
        return report.getMessages().stream()
                .map(ValidationReport.Message::getMessage)
                .reduce("", (s, m) -> s + m);
    }
}
