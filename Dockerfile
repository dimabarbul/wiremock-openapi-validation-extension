FROM wiremock/wiremock:3.8.0-1
COPY target/wiremock-openapi-validation-extension-*.jar /var/wiremock/extensions
