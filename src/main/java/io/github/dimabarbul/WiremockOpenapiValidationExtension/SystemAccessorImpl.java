package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.util.Optional;

class SystemAccessorImpl implements SystemAccessor {
    @Override
    public Optional<String> getEnvironmentVariable(final ValidationParameter parameter) {
        return Optional.ofNullable(System.getenv(parameter.envName()));
    }

    @Override
    public Optional<String> getSystemProperty(final ValidationParameter parameter) {
        return Optional.ofNullable(System.getProperty(parameter.systemPropertyName()));
    }
}
