package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.util.stream.Collectors;

import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.net.MediaType;

final class ErrorResponseBuilder {

    public static Response buildResponse(final int statusCode, final ValidationReport requestReport, final ValidationReport responseReport) {
        return Response.response()
                .status(statusCode)
                .headers(new HttpHeaders(new HttpHeader("Content-Type", MediaType.HTML_UTF_8.toString())))
                .body(buildBody(requestReport, responseReport))
                .build();
    }

    private static String buildBody(final ValidationReport requestReport, final ValidationReport responseReport) {
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
                                        m -> "\t<li>" + getErrorHtml(m) + "</li>\n")
                                .collect(Collectors.joining()) +
                        "</ul>\n" :
                "<b>No errors</b>\n";
    }

    private static String getErrorHtml(final ValidationReport.Message message) {
        return String.format("<i>[%s]</i> %s", message.getKey(), message.getMessage());
    }
}
