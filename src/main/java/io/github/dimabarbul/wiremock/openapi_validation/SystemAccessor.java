package io.github.dimabarbul.wiremock.openapi_validation;

import java.util.Optional;

interface SystemAccessor {
    SystemAccessor Instance = new SystemAccessorImpl();

    Optional<String> getEnvironmentVariable(final ValidationParameter parameter);
    Optional<String> getSystemProperty(final ValidationParameter parameter);
}
