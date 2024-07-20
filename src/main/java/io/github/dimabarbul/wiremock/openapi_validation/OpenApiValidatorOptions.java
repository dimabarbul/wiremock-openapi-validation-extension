package io.github.dimabarbul.wiremock.openapi_validation;

import com.google.common.collect.ImmutableList;

final class OpenApiValidatorOptions {
    private final ImmutableList<String> ignoredErrors;

    private OpenApiValidatorOptions(final ImmutableList<String> ignoredErrors) {
        this.ignoredErrors = ignoredErrors;
    }

    public static OpenApiValidatorOptions fromExtensionOptions(final ExtensionOptions options) {
        return new OpenApiValidatorOptions(options.getIgnoredErrors());
    }

    public ImmutableList<String> getIgnoredErrors() {
        return ignoredErrors;
    }
}
