/*
 * Copyright 2025 Dmitriy Barbul
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

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.File;
import java.util.List;

/**
 * WireMock response transformer that validates request and response against OpenAPI file. It returns original response
 * if request and response are valid, otherwise it replaces response with one describing what exactly went wrong.
 */
public final class ValidationResponseTransformer implements ResponseTransformerV2 {

    private static final List<String> DEFAULT_OPENAPI_FILE_PATHS = List.of(
            "openapi.json",
            "openapi.yaml",
            "openapi.yml",
            "/home/wiremock/openapi.json",
            "/home/wiremock/openapi.yaml",
            "/home/wiremock/openapi.yml");

    private final ExtensionOptions options;
    private final OpenApiValidator globalValidator;

    /**
     * Create a new instance of {@link ValidationResponseTransformer} with options configured by environment variables
     * and system properties.
     */
    @SuppressWarnings("unused")
    public ValidationResponseTransformer() {
        this(ExtensionOptions.fromSystemParameters());
    }

    @Override
    public void start() {
        if (options.shouldPrintConfiguration()) {
            printConfiguration();
        }
    }

    /**
     * Create a new instance of {@link ValidationResponseTransformer}.
     *
     * @param options Options to use
     */
    public ValidationResponseTransformer(final ExtensionOptions options) {
        this.options = guessOpenapiFilePathIfAbsent(options);
        this.globalValidator = OpenApiValidator.create(this.options);
    }

    @Override
    public Response transform(final Response response, final ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        final LoggedRequest request = serveEvent.getRequest();
        final Response extendedResponse = extendResponse(response);

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);
        final ExtensionOptions mergedOptions =
                ExtensionOptions.builder(options).mergeWith(parameters).build();
        final OpenApiValidator validator =
                globalValidator.withOptions(OpenApiValidatorOptions.fromExtensionOptions(mergedOptions));
        final ValidationResult requestValidationResult = validator.validateRequest(request);
        final ValidationResult responseValidationResult = validator.validateResponse(request, extendedResponse);

        if (requestValidationResult.hasErrors() || responseValidationResult.hasErrors()) {
            final Response errorResponse = ErrorResponseBuilder.buildResponse(
                    mergedOptions.getFailureStatusCode(), requestValidationResult, responseValidationResult);
            log(request, extendedResponse, errorResponse);
            return errorResponse;
        }

        return extendedResponse;
    }

    @Override
    public String getName() {
        return "openapi-validation";
    }

    private static ExtensionOptions guessOpenapiFilePathIfAbsent(final ExtensionOptions options) {
        return options.getOpenapiFilePath() == null
                ? ExtensionOptions.builder(options)
                        .withOpenapiFilePath(getFirstExistingFile())
                        .build()
                : options;
    }

    private static String getFirstExistingFile() {
        for (final String filePath : DEFAULT_OPENAPI_FILE_PATHS) {
            if (new File(filePath).exists()) {
                return filePath;
            }
        }

        throw new RuntimeException(String.format(
                "Cannot find OpenAPI file. Checked locations: %s", String.join(", ", DEFAULT_OPENAPI_FILE_PATHS)));
    }

    private static void log(final LoggedRequest request, final Response response, final Response errorResponse) {
        notifier()
                .error(String.format(
                        "OpenAPI validation error\n\n** Request **:\n%s\n\n** Response **:\n%s\n\n** Validation response **:\n%s",
                        prettifyForOutput(request), prettifyForOutput(response), prettifyForOutput(errorResponse)));
    }

    private static String prettifyForOutput(final LoggedRequest request) {
        return String.format(
                "%s %s\n\n%s\n%s",
                request.getMethod(), request.getUrl(), request.getHeaders(), request.getBodyAsString());
    }

    private static String prettifyForOutput(final Response response) {
        return String.format(
                "HTTP/1.1 %d\n\n%s\n%s", response.getStatus(), response.getHeaders(), response.getBodyAsString());
    }

    private void printConfiguration() {
        System.out.println("------------------------------------");
        System.out.println("|   OpenAPI Validation Extension   |");
        System.out.println("------------------------------------");
        System.out.println();
        System.out.println("OpenAPI:                      " + options.getOpenapiFilePath());
        System.out.println("Validator name:               " + options.getValidatorName());
        System.out.println("Is invalid OpenAPI allowed:   " + options.isInvalidOpenapiAllowed());
        System.out.println("Failure status code:          " + options.getFailureStatusCode());
        if (options.getIgnoredErrors().isEmpty()) {
            System.out.println("Ignored errors:               <none>");
        } else {
            System.out.print("Ignored errors:               ");
            System.out.println(String.join("\n                              ", options.getIgnoredErrors()));
        }
        System.out.println();
    }

    private Response extendResponse(final Response response) {
        final String defaultContentType = options.getDefaultResponseContentType();
        if (response.getHeaders().getContentTypeHeader().isPresent() || defaultContentType == null) {
            return response;
        }

        return Response.Builder.like(response)
                .headers(response.getHeaders().plus(HttpHeader.httpHeader(ContentTypeHeader.KEY, defaultContentType)))
                .build();
    }
}
