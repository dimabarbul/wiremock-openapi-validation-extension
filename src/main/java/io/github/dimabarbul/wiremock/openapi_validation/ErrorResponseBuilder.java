package io.github.dimabarbul.wiremock.openapi_validation;

import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.net.MediaType;

final class ErrorResponseBuilder {

    private ErrorResponseBuilder() {
    }

    public static Response buildResponse(final int statusCode,
                                         final ValidationResult requestValidationResult,
                                         final ValidationResult responseValidationResult) {
        return Response.response()
                .status(statusCode)
                .headers(new HttpHeaders(new HttpHeader("Content-Type", MediaType.HTML_UTF_8.toString())))
                .body(buildBody(requestValidationResult, responseValidationResult))
                .build();
    }

    private static String buildBody(final ValidationResult requestValidationResult,
                                    final ValidationResult responseValidationResult) {
        return "<h1>Validation against OpenAPI failed</h1>\n"
                + "<h2>Request Errors</h2>\n"
                + getErrorsHtml(requestValidationResult)
                + "<h2>Response Errors</h2>\n"
                + getErrorsHtml(responseValidationResult);
    }

    private static String getErrorsHtml(final ValidationResult validationResult) {
        return validationResult.hasErrors()
                ? "<ul>\n"
                        + validationResult.getErrors().stream().map(m -> "\t<li>" + getErrorHtml(m) + "</li>\n")
                                .collect(Collectors.joining())
                        + "</ul>\n"
                : "<b>No errors</b>\n";
    }

    private static String getErrorHtml(final ValidationResult.Error error) {
        return String.format("<i>[%s]</i> %s", error.getKey(), error.getMessage());
    }
}
