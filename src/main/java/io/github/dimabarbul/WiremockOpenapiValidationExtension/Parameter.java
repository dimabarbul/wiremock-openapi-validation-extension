package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import com.google.common.base.CaseFormat;

class Parameter {
    private final String name;

    /**
     * @param name Name of parameter in lower_snake_case or in UPPER_SNAKE_CASE.
     */
    public Parameter(final String name) {
        this.name = name;
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
