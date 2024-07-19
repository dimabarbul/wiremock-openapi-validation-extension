package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import java.io.File;
import java.util.List;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

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

    private final ExtensionOptions options;
    private final OpenApiValidator globalValidator;

    @SuppressWarnings("unused")
    public ValidationResponseTransformer() {
        this(getDefaultOptions());
    }

    public ValidationResponseTransformer(final ExtensionOptions options) {
        this.options = options;
        this.globalValidator = OpenApiValidator.create(this.options);
    }

    @Override
    public Response transform(final Response response, final ServeEvent serveEvent) {
        if (!serveEvent.getWasMatched()) {
            return response;
        }

        LoggedRequest request = serveEvent.getRequest();

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);
        final ExtensionOptions mergedOptions = ExtensionOptions.builder(options)
                .mergeWith(parameters)
                .build();
        final OpenApiValidator validator = globalValidator.withOptions(OpenApiValidatorOptions.fromExtensionOptions(mergedOptions));
        final ValidationResult requestValidationResult = validator.validateRequest(request);
        final ValidationResult responseValidationResult = validator.validateResponse(request, response);

        if (requestValidationResult.hasErrors() || responseValidationResult.hasErrors()) {
            Response errorResponse = ErrorResponseBuilder.buildResponse(
                    mergedOptions.getFailureStatusCode(), requestValidationResult, responseValidationResult);
            log(request, response, errorResponse);
            return errorResponse;
        }

        return response;
    }

    @Override
    public String getName() {
        return "openapi-validation";
    }

    private static ExtensionOptions getDefaultOptions() {
        ExtensionOptions options = ExtensionOptions.fromSystemParameters();
        if (options.getOpenapiFilePath() == null) {
            options = ExtensionOptions.builder(options)
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
