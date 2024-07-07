package com.github.dimabarbul.WiremockOpenapiValidationExtension;

import java.util.Arrays;

import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;

public final class StandaloneRunner {
    public static void main(String[] args) {
        String[] argsWithThisExtension = Arrays.copyOf(args, args.length + 1);
        argsWithThisExtension[args.length] = "--extension=" + ValidationResponseFilter.class.getName();
        new WireMockServerRunner().run(argsWithThisExtension);
    }
}
