package io.github.dimabarbul.wiremock.openapi_validation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

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
