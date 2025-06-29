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

import static java.util.Objects.requireNonNull;

import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.HashMap;
import java.util.Map;

final class ValidationTransformerParameters {

    private final Integer failureStatusCode;
    private final Map<String, Boolean> ignoredErrors;

    private ValidationTransformerParameters(final Integer failureStatusCode, final Map<String, Boolean> ignoredErrors) {
        this.failureStatusCode = failureStatusCode;
        this.ignoredErrors = requireNonNull(ignoredErrors);
    }

    public Integer getFailureStatusCode() {
        return failureStatusCode;
    }

    public Map<String, Boolean> getIgnoredErrors() {
        return ignoredErrors;
    }

    public static ValidationTransformerParameters fromServeEvent(final ServeEvent serveEvent) {
        final Parameters transformerParameters = serveEvent.getTransformerParameters();
        final Integer failureStatusCode =
                transformerParameters.getInt(ValidationParameter.FAILURE_STATUS_CODE.transformerParameterName(), null);
        final Metadata ignoreErrorsMetadata =
                transformerParameters.getMetadata(ValidationParameter.IGNORE_ERRORS.transformerParameterName(), null);
        final Map<String, Boolean> ignoredErrors;
        if (ignoreErrorsMetadata != null) {
            ignoredErrors = new HashMap<>(ignoreErrorsMetadata.size());
            for (final String key : ignoreErrorsMetadata.keySet()) {
                ignoredErrors.put(key, ignoreErrorsMetadata.getBoolean(key));
            }
        } else {
            ignoredErrors = Map.of();
        }

        return new ValidationTransformerParameters(failureStatusCode, ignoredErrors);
    }
}
