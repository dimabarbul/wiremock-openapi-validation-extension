/*
 * Copyright 2024 Dmitriy Barbul
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

import com.google.common.base.CaseFormat;

final class ValidationParameter {

    public static final ValidationParameter OPENAPI_FILE_PATH = new ValidationParameter("openapi_validation_file_path");
    public static final ValidationParameter ALLOW_INVALID_OPENAPI = new ValidationParameter("openapi_validation_allow_invalid_openapi");
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
