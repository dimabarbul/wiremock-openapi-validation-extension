package io.github.dimabarbul.wiremock.openapi_validation;

import java.util.Optional;

interface SystemAccessor {
    SystemAccessor INSTANCE = new SystemAccessorImpl();

    Optional<String> getEnvironmentVariable(ValidationParameter parameter);
    Optional<String> getSystemProperty(ValidationParameter parameter);
}
