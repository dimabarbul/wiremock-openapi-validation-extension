package io.github.dimabarbul.wiremock.openapi_validation;

import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

interface OpenApiValidator {
    String ATLASSIAN_VALIDATOR_NAME = "atlassian";

    static OpenApiValidator create(final ExtensionOptions options) {
        final String validatorName = options.getValidatorName();

        if (ATLASSIAN_VALIDATOR_NAME.equals(validatorName)) {
            return new AtlassianOpenApiValidator(
                    options.getOpenapiFilePath(),
                    options.isInvalidOpenapiAllowed(),
                    OpenApiValidatorOptions.fromExtensionOptions(options));
        }

        throw new IllegalArgumentException("Unknown validator name \"" + validatorName + "\".");
    }

    OpenApiValidator withOptions(OpenApiValidatorOptions options);

    ValidationResult validateRequest(LoggedRequest request);

    ValidationResult validateResponse(LoggedRequest request, Response response);
}
