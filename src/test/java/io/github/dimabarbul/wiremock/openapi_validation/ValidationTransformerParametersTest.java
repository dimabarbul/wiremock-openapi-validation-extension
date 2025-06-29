/*
 * Copyright 2025 Dmitriy Barbul
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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationTransformerParametersTest {
    @Test
    public void testFromServeEventWhenNothingIsSet() throws JsonProcessingException {
        final ServeEvent serveEvent = new ObjectMapper().readValue("{}", ServeEvent.class);

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        assertThat(parameters).isNotNull();
        assertThat(parameters.getFailureStatusCode()).isNull();
        assertThat(parameters.getIgnoredErrors()).isEmpty();
    }

    @Test
    void testFromServeEventWhenEverythingIsSet() throws JsonProcessingException {
        final String json = "{"
                + "    \"mapping\": {"
                + "        \"response\": {"
                + "            \"transformerParameters\": {"
                + "                \"openapiValidationFailureStatusCode\": 418,"
                + "                \"openapiValidationIgnoreErrors\": {"
                + "                    \"error1\": true,"
                + "                    \"error2\": false"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}";
        final ServeEvent serveEvent = new ObjectMapper().readValue(json, ServeEvent.class);

        final ValidationTransformerParameters parameters = ValidationTransformerParameters.fromServeEvent(serveEvent);

        assertThat(parameters).isNotNull();
        assertThat(parameters.getFailureStatusCode()).isEqualTo(418);
        assertThat(parameters.getIgnoredErrors())
                .containsExactly(Map.entry("error1", true), Map.entry("error2", false));
    }
}
