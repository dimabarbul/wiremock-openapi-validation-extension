package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

interface OpenApiValidator {
    String ATLASSIAN_VALIDATOR_NAME = "atlassian";

    static OpenApiValidator create(final ExtensionOptions options) {
        final String validatorName = options.getValidatorName();

        if (ATLASSIAN_VALIDATOR_NAME.equals(validatorName)) {
            return new AtlassianOpenApiValidator(
                    options.getOpenapiFilePath(),
                    options.shouldIgnoreOpenapiErrors(),
                    OpenApiValidatorOptions.fromExtensionOptions(options));
        }

        throw new IllegalArgumentException("Unknown validator name \"" + validatorName + "\".");
    }

    OpenApiValidator withOptions(final OpenApiValidatorOptions options);

    ValidationResult validateRequest(final LoggedRequest request);

    ValidationResult validateResponse(final LoggedRequest request, final Response response);
}