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
 * Options for the extension, used by different classes throughout the code.
 * Use {@link ExtensionOptions#builder} or {@link ExtensionOptions#fromSystemParameters()} to construct.
 */
public class ExtensionOptions {

    private static final int DEFAULT_FAILURE_STATUS_CODE = 500;

    private final String openapiFilePath;
    private final boolean ignoreOpenapiErrors;
    private final String validatorName;
    private final int failureStatusCode;
    private final ImmutableList<String> ignoredErrors;

    private ExtensionOptions(final String openapiFilePath,
                             final boolean ignoreOpenapiErrors,
                             final String validatorName,
                             final int failureStatusCode,
                             final ImmutableList<String> ignoredErrors) {
        this.openapiFilePath = openapiFilePath;
        this.ignoreOpenapiErrors = ignoreOpenapiErrors;
        this.validatorName = validatorName;
        this.failureStatusCode = failureStatusCode;
        this.ignoredErrors = requireNonNull(ignoredErrors);
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
    public static Builder builder(final ExtensionOptions options) {
        return new Builder(options);
    }

    public static ExtensionOptions fromSystemParameters() {
        return fromSystemParameters(SystemAccessor.Instance);
    }

    static ExtensionOptions fromSystemParameters(
            final SystemAccessor systemAccessor) {
        Builder builder = builder();
        getGlobalParameter(systemAccessor, ValidationParameter.OPENAPI_FILE_PATH).ifPresent(builder::withOpenapiFilePath);
        getGlobalParameter(systemAccessor, ValidationParameter.VALIDATOR_NAME).ifPresent(builder::withValidatorName);
        getGlobalParameter(systemAccessor, ValidationParameter.IGNORE_OPENAPI_ERRORS)
                .map(Boolean::parseBoolean)
                .ifPresent(builder::ignoreOpenapiErrors);
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

    public boolean shouldIgnoreOpenapiErrors() {
        return ignoreOpenapiErrors;
    }

    public int getFailureStatusCode() {
        return failureStatusCode;
    }

    public ImmutableList<String> getIgnoredErrors() {
        return ignoredErrors;
    }

    public String getValidatorName() {
        return validatorName;
    }

    public static final class Builder {

        private String openapiFilePath = null;
        private boolean ignoreOpenapiErrors = false;
        private String validatorName = "atlassian";
        private int failureStatusCode = DEFAULT_FAILURE_STATUS_CODE;
        private List<String> ignoredErrors = List.of();

        public Builder() {
        }

        public Builder(final ExtensionOptions options) {
            openapiFilePath = options.getOpenapiFilePath();
            validatorName = options.getValidatorName();
            ignoreOpenapiErrors = options.shouldIgnoreOpenapiErrors();
            failureStatusCode = options.getFailureStatusCode();
            ignoredErrors = options.getIgnoredErrors();
        }

        public Builder withOpenapiFilePath(final String openapiFilePath) {
            this.openapiFilePath = openapiFilePath;
            return this;
        }

        public Builder withFailureStatusCode(final int failureStatusCode) {
            this.failureStatusCode = failureStatusCode;
            return this;
        }

        public Builder withIgnoredErrors(final List<String> ignoredErrors) {
            this.ignoredErrors = ignoredErrors;
            return this;
        }

        public Builder withValidatorName(final String validatorName) {
            this.validatorName = validatorName;
            return this;
        }

        public Builder ignoreOpenapiErrors(boolean ignoreOpenapiErrors) {
            this.ignoreOpenapiErrors = ignoreOpenapiErrors;
            return this;
        }

        public String getOpenapiFilePath() {
            return openapiFilePath;
        }

        public ExtensionOptions build() {
            return new ExtensionOptions(
                    openapiFilePath, ignoreOpenapiErrors, validatorName, failureStatusCode, ImmutableList.copyOf(ignoredErrors));
        }

        Builder mergeWith(final ValidationTransformerParameters parameters) {
            mergeFailureStatusCode(parameters);
            mergeIgnoredErrors(parameters);

            return this;
        }

        private void mergeFailureStatusCode(final ValidationTransformerParameters parameters) {
            Optional.ofNullable(parameters.getFailureStatusCode()).ifPresent(this::withFailureStatusCode);
        }

        private void mergeIgnoredErrors(final ValidationTransformerParameters parameters) {
            Map<String, Boolean> ignoredErrorsFromParameters = parameters.getIgnoredErrors();

            if (!ignoredErrorsFromParameters.isEmpty()) {
                final Set<String> resultIgnoredErrors = new HashSet<>(ignoredErrors);
                ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .forEach(resultIgnoredErrors::add);
                ignoredErrorsFromParameters.entrySet()
                        .stream()
                        .filter(e -> !e.getValue())
                        .map(Map.Entry::getKey)
                        .forEach(resultIgnoredErrors::remove);

                ignoredErrors = List.copyOf(resultIgnoredErrors);
            }
        }
    }
}
