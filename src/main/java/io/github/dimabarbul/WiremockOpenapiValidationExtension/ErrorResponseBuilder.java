package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.util.stream.Collectors;

import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;

final class ErrorResponseBuilder {
    private static final int VALIDATION_FAILED_STATUS_CODE = 500;

    public static Response buildResponse(final ValidationReport requestReport, final ValidationReport responseReport) {
        return Response.response()
                .status(VALIDATION_FAILED_STATUS_CODE)
                .headers(new HttpHeaders(new HttpHeader("Content-Type", "text/html")))
                .body(buildBody(requestReport, responseReport))
                .build();
    }

    private static String buildBody(final ValidationReport requestReport, final  ValidationReport responseReport) {
        return "<h1>Validation against OpenAPI failed</h1>\n" +
                "<h2>Request Errors</h2>\n" +
                getErrorsHtml(requestReport) +
                "<h2>Response Errors</h2>\n" +
                getErrorsHtml(responseReport);

    }

    private static String getErrorsHtml(final ValidationReport report) {
        return report.hasErrors() ?
                "<ul>\n" +
                        report.getMessages().stream().map(
                                        m -> "\t<li>" + m.getMessage() + "</li>\n")
                                .collect(Collectors.joining()) +
                        "</ul>\n" :
                "<b>No errors</b>\n";
    }
}