package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static java.util.Objects.requireNonNull;

import com.google.common.base.CaseFormat;

class ValidationParameter {

    public static final ValidationParameter OPENAPI_FILE_PATH = new ValidationParameter("openapi_validation_file_path");
    public static final ValidationParameter FAILURE_STATUS_CODE = new ValidationParameter("openapi_validation_failure_status_code");
    public static final ValidationParameter IGNORE_ERRORS = new ValidationParameter("openapi_validation_ignore_errors");
    public static final ValidationParameter VALIDATOR_NAME = new ValidationParameter("openapi_validation_validator_name");

    /**
     * Name in lower_snake_case format.
     */
    private final String name;

    /**
     * @param name Name of parameter in lower_snake_case or in UPPER_SNAKE_CASE.
     */
    private ValidationParameter(final String name) {
        requireNonNull(name);
        this.name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    public String envName() {
        return this.name.toUpperCase();
    }

    public String systemPropertyName() {
        return this.name.toLowerCase();
    }

    public String transformerParameterName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
    }
}
