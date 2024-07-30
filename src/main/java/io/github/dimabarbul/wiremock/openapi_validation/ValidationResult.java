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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

class ValidationResult {

    private final ImmutableList<Error> errors;

    public static Builder builder() {
        return new Builder();
    }

    ValidationResult(final ImmutableList<Error> errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public ImmutableList<Error> getErrors() {
        return errors;
    }

    static class Error {

        private final String key;
        private final String message;

        Error(final String key, final String message) {
            this.key = key;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }
    }

    static class Builder {
        private final List<Error> errors = new ArrayList<>();

        public Builder addError(final String key, final String message) {
            errors.add(new Error(key, message));
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(ImmutableList.copyOf(errors));
        }
    }
}
