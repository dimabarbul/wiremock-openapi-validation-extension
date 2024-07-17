package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * Options for {@link ValidationResponseTransformer}.
 * Use {@link ValidationResponseTransformerOptions#builder} or
 * {@link ValidationResponseTransformerOptions#fromSystemParameters()} to construct.
 */
public class ValidationResponseTransformerOptions {

    private static final int DEFAULT_FAILURE_STATUS_CODE = 500;

    private final String openapiFilePath;
    private final int failureStatusCode;
    private final ImmutableList<String> ignoredErrors;

    private ValidationResponseTransformerOptions(final String openapiFilePath,
                                                 final int failureStatusCode,
                                                 final ImmutableList<String> ignoredErrors) {
        this.failureStatusCode = failureStatusCode;
        this.ignoredErrors = requireNonNull(ignoredErrors);
        this.openapiFilePath = openapiFilePath;
    }

    /**
     * Create new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create new builder prepopulated with the options.
     *
     * @param options Options to copy from
     *
     * @return Builder prepopulated with data from the options
     */
    public static Builder builder(final ValidationResponseTransformerOptions options) {
        return new Builder(options);
    }

    public static ValidationResponseTransformerOptions fromSystemParameters() {
        return fromSystemParameters(SystemAccessor.Instance);
    }

    static ValidationResponseTransformerOptions fromSystemParameters(
            final SystemAccessor systemAccessor) {
        Builder builder = builder();
        getGlobalParameter(systemAccessor, ValidationParameter.OPENAPI_FILE_PATH).ifPresent(builder::withOpenapiFilePath);
        getGlobalParameter(systemAccessor, ValidationParameter.FAILURE_STATUS_CODE)
                .map(Integer::parseInt)
                .ifPresent(builder::withFailureStatusCode);
        getGlobalParameter(systemAccessor, ValidationParameter.IGNORE_ERRORS)
                .map(e -> Arrays.stream(e.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .ifPresent(builder::withIgnoredErrors);
        return builder.build();
    }

    private static Optional<String> getGlobalParameter(final SystemAccessor systemAccessor,
                                                       final ValidationParameter parameter) {
        return systemAccessor.getSystemProperty(parameter)
                .or(() -> systemAccessor.getEnvironmentVariable(parameter));
    }

    public String getOpenapiFilePath() {
        return openapiFilePath;
    }

    public int getFailureStatusCode() {
        return failureStatusCode;
    }

    public ImmutableList<String> getIgnoredErrors() {
        return ignoredErrors;
    }

    public static final class Builder {

        private String openapiFilePath = null;
        private int failureStatusCode = DEFAULT_FAILURE_STATUS_CODE;
        private List<String> ignoredErrors = List.of();

        public Builder() {
        }

        public Builder(final ValidationResponseTransformerOptions options) {
            openapiFilePath = options.getOpenapiFilePath();
            failureStatusCode = options.getFailureStatusCode();
            ignoredErrors = options.getIgnoredErrors();
        }

        public ValidationResponseTransformerOptions.Builder withOpenapiFilePath(final String openapiFilePath) {
            this.openapiFilePath = openapiFilePath;
            return this;
        }

        public ValidationResponseTransformerOptions.Builder withFailureStatusCode(final int failureStatusCode) {
            this.failureStatusCode = failureStatusCode;
            return this;
        }

        public ValidationResponseTransformerOptions.Builder withIgnoredErrors(final List<String> ignoredErrors) {
            this.ignoredErrors = ignoredErrors;
            return this;
        }

        public ValidationResponseTransformerOptions build() {
            return new ValidationResponseTransformerOptions(
                    openapiFilePath, failureStatusCode, ImmutableList.copyOf(ignoredErrors));
        }

        Builder mergeWith(final ValidationTransformerParameters parameters) {
            Optional.ofNullable(parameters.getFailureStatusCode()).ifPresent(this::withFailureStatusCode);
            Map<String, Boolean> ignoredErrorsFromParameters = parameters.getIgnoredErrors();

            final Set<String> resultIgnoredErrors = new HashSet<>(ignoredErrors);

            if (!ignoredErrorsFromParameters.isEmpty()) {
                resultIgnoredErrors.addAll(ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));
                ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(e -> !e.getValue())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList())
                        .forEach(resultIgnoredErrors::remove);

                withIgnoredErrors(List.copyOf(resultIgnoredErrors));
            }

            return this;
        }

    }
}
