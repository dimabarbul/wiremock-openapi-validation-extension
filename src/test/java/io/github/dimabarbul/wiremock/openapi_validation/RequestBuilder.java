package io.github.dimabarbul.wiremock.openapi_validation;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.net.MediaType;

class RequestBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Request postJsonRequest(final String url, final Object body) {
        try {
            return ImmutableRequest.create()
                    .withMethod(RequestMethod.POST)
                    .withAbsoluteUrl(url)
                    .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                    .withBody(mapper.writeValueAsBytes(body))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Request postJsonRequest(final String url, final String body) {
            return ImmutableRequest.create()
                    .withMethod(RequestMethod.POST)
                    .withAbsoluteUrl(url)
                    .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                    .withBody(body.getBytes(StandardCharsets.UTF_8))
                    .build();
    }

    public static Request getRequest(final String url) {
        return ImmutableRequest.create()
                .withMethod(RequestMethod.GET)
                .withAbsoluteUrl(url)
                .build();
    }

    public static Request deleteRequest(final String url) {
        return ImmutableRequest.create()
                .withMethod(RequestMethod.DELETE)
                .withAbsoluteUrl(url)
                .build();
    }
}
