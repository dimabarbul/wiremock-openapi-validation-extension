package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

class Openapi4jOpenApiValidator implements OpenApiValidator {
    @Override
    public OpenApiValidator withOptions(final OpenApiValidatorOptions options) {
        return null;
    }

    @Override
    public ValidationResult validateRequest(final LoggedRequest request) {
        return null;
    }

    @Override
    public ValidationResult validateResponse(final LoggedRequest request, final Response response) {
        return null;
    }
    /*try {
            LoggedRequest loggedRequest = request;
            OpenApi3 openApi = new OpenApi3Parser().parse(
                    new File(options.getOpenapiFilePath()),
                    false);
            RequestValidator v = new RequestValidator(openApi);
            HttpHeaders headers = loggedRequest.getHeaders();
            org.openapi4j.operation.validator.model.Request r = new DefaultRequest.Builder(
                    loggedRequest.getUrl(), org.openapi4j.operation.validator.model.Request.Method.GET)
                    .query(new URI(loggedRequest.getAbsoluteUrl()).getRawQuery())
                    .headers(headers.keys()
                            .stream()
                            .collect(Collectors.toMap(
                                    key -> key,
                                    key -> headers.getHeader(key).values())))
                    .body(Body.from(loggedRequest.getBody()))
                    .build();
            final RequestParameters validationResult = v.validate(r, new ValidationData<>());
            System.out.println(validationResult);
        } catch (Exception e) {
            // ignore, test
            e.printStackTrace();
        }*/
}
