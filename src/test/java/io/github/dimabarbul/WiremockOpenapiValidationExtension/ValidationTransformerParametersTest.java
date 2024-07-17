package io.github.dimabarbul.WiremockOpenapiValidationExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

class ValidationTransformerParametersTest {
    @Test
    public void testFromServeEventWhenNothingIsSet()
            throws JsonProcessingException {
        final ServeEvent serveEvent = new ObjectMapper().readValue("{}", ServeEvent.class);

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        assertThat(parameters).isNotNull();
        assertThat(parameters.getFailureStatusCode()).isNull();
        assertThat(parameters.getIgnoredErrors()).isEmpty();
    }

    @Test
    void testFromServeEventWhenEverythingIsSet()
            throws JsonProcessingException {
        final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        final String json = jsonFactory.objectNode()
                .set("mapping", jsonFactory.objectNode()
                        .set("response", jsonFactory.objectNode()
                                .set("transformerParameters", jsonFactory.objectNode()
                                        .setAll(Map.<String, JsonNode>of(
                                                "openapiValidationFailureStatusCode", jsonFactory.numberNode(418),
                                                "openapiValidationIgnoreErrors", jsonFactory.objectNode()
                                                        .setAll(Map.<String, JsonNode>of(
                                                                "error1", jsonFactory.booleanNode(true),
                                                                "error2", jsonFactory.booleanNode(false))))))))
                .toString();
        final ServeEvent serveEvent = new ObjectMapper().readValue(json, ServeEvent.class);

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        assertThat(parameters).isNotNull();
        assertThat(parameters.getFailureStatusCode()).isEqualTo(418);
        assertThat(parameters.getIgnoredErrors()).containsExactly(
                Map.entry("error1", true),
                Map.entry("error2", false));
    }
}