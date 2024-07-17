package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

class ValidationTransformerParameters {

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
        final Integer failureStatusCode = transformerParameters.getInt(
                ValidationParameter.FAILURE_STATUS_CODE.transformerParameterName(), null);
        final Metadata ignoreErrorsMetadata = transformerParameters.getMetadata(
                ValidationParameter.IGNORE_ERRORS.transformerParameterName(), null);
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
