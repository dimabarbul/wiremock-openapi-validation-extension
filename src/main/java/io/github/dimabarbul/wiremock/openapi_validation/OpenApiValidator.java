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
