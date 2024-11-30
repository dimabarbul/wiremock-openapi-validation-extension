# OpenAPI Validation Extension for WireMock

OpenAPI Validation extension for WireMock provides validation for request and/or response against OpenAPI file. WireMock standalone does not provide such functionality out-of-the-box, but it is useful to know that stubs you're using conform with API schema.

When the request is matched by some stub and response is about to be sent to the client, the extension validates request and response against the OpenAPI file provided and, in case of some errors, replaces the response with the one describing the errors found.

## Usage

The extension requires OpenAPI file to be provided in order to work, otherwise, the application won't start. You can create one in current folder (where you'll start the application) or you can specify its name in environment variable `OPENAPI_VALIDATION_FILE_PATH` or Java system property `openapi_validation_file_path`. It can be URL, so defining environment variable `OPENAPI_VALIDATION_FILE_PATH` with value `https://petstore3.swagger.io/api/v3/openapi.json` will make the extension use [swagger petstore](https://github.com/swagger-api/swagger-petstore) sample OpenAPI file. For more info check [configuration section](#openapi-file-path).

The extension is written dependent in WireMock standalone **3.10.0**. It might or might not work with other versions.

### In Java

You need to add dependency on the extension.

For Maven:

```xml
<dependency>
    <groupId>io.github.dimabarbul</groupId>
    <artifactId>wiremock-openapi-validation-extension</artifactId>
    <version>1.1.0</version>
</dependency>
```

For Gradle:

```groovy
dependencies {
    implementation 'io.github.dimabarbul:wiremock-openapi-validation-extension:1.1.0'
}
```

And that's it: the extension uses [service loading](https://wiremock.org/docs/extending-wiremock/#extension-registration-via-service-loading) approach, so it will be registered in WireMock server automatically.

### Using Docker Image

The extension is available as docker image. It uses wiremock standalone image and just adds jar with the extension. Example:

```bash
# run with local file
docker run -it --rm \
  -p 8080:8080 \
  -v $PWD/openapi.json:/home/wiremock/openapi.json \
  -v $PWD/mappings:/home/wiremock/mappings \
  dimabarbul/wiremock-openapi-validation:latest

# run with URL
docker run -it --rm \
  -p 8080:8080 \
  -e OPENAPI_VALIDATION_FILE_PATH=https://petstore3.swagger.io/api/v3/openapi.json \
  -v $PWD/mappings:/home/wiremock/mappings \
  dimabarbul/wiremock-openapi-validation:latest
```

More information is in dedicated document [Docker.md](./Docker.md).

### Using Jar File

The extension depends on wiremock package to be provided, so to run it you need to download wiremock standalone and the extension jars and run them:

```bash
# download jars
wget https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.10.0/wiremock-standalone-3.10.0.jar
wget https://repo1.maven.org/maven2/io/github/dimabarbul/wiremock-openapi-validation-extension/1.1.0/wiremock-openapi-validation-extension-1.1.0-jar-with-dependencies.jar
# run with default file path
java -cp "wiremock-openapi-validation-extension-1.1.0-jar-with-dependencies.jar:wiremock-standalone-3.10.0.jar" \
  wiremock.Run
# run with specified file path
java -cp "wiremock-openapi-validation-extension-1.1.0-jar-with-dependencies.jar:wiremock-standalone-3.10.0.jar" \
  -Dopenapi_validation_file_path=https://petstore3.swagger.io/api/v3/openapi.json \
  wiremock.Run
```

### From Source

Running from source requires you to have Maven and Java SDK installed.

```bash
mvn compile
# run with default file path
mvn exec:java \
  -Dexec.mainClass=wiremock.Run \
  -Dexec.classpathScope="compile"
# run with specified file path
mvn exec:java \
  -Dexec.mainClass=wiremock.Run \
  -Dexec.classpathScope="compile" \
  -Dopenapi_validation_file_path=https://petstore3.swagger.io/api/v3/openapi.json
  -Dexec.args='--verbose'
```

## Configuration

The extension behavior can be configured using environment variables, system properties and - for some settings - in [transformer parameters](https://wiremock.org/docs/extensibility/transforming-responses/#parameters). If environment variable AND system property are set, the latter wins, they are not merged for any case. Transformer parameters have the highest priority. Usually they override the value, but for [ignoring errors](#ignore-errors) transformer parameters are merged with environment variable or system property.

When the extension is used in Java code, the configuration can be provided as constructor argument:

```java
new WireMockServer(wireMockConfig()
        .extensions(new ValidationResponseTransformer(ExtensionOptions.builder()
                .withOpenapiFilePath("my-file.json")
                .withInvalidOpenapiAllowed(true)
                .withValidatorName("atlassian")
                .withFailureStatusCode(418)
                .withIgnoredErrors(List.of("validation.response.status.unknown"))
                .build())));
```

### Print Config

| Where to Set          | Name                            |
|-----------------------|---------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_PRINT_CONFIG |
| System Property       | openapi_validation_print_config |
| Transformer Parameter | N/A                             |

**Default**: *false*

Flag indicating whether the extension should print configuration to stdout on start. By default, it prints nothing, but for debugging purposes it might be useful to check what OpenAPI the extension uses, what is failure status code etc.

### OpenAPI File Path

| Where to Set          | Name                         |
|-----------------------|------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_FILE_PATH |
| System Property       | openapi_validation_file_path |
| Transformer Parameter | N/A                          |

File path of OpenAPI file to use. File can be in JSON or YAML format. It can be even a URL. If the option is not set, then the first existing file will be used from the list below (in the same order):

- openapi.json
- openapi.yaml
- openapi.yml
- /home/wiremock/openapi.json
- /home/wiremock/openapi.yaml
- /home/wiremock/openapi.yml

If file cannot be found, an exception is thrown, so the application won't start.

### Allow Invalid OpenAPI File

| Where to Set          | Name                                     |
|-----------------------|------------------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_ALLOW_INVALID_OPENAPI |
| System Property       | openapi_validation_allow_invalid_openapi |
| Transformer Parameter | N/A                                      |

**Default**: *false*

Sometimes it might happen that the file is considered invalid by a [validator](#validator-name) and cannot be loaded. If you can't or don't want to change the OpenAPI file, you can instruct the extension to ignore all errors that appear during loading the file.

### Validator Name

| Where to Set          | Name                              |
|-----------------------|-----------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_VALIDATOR_NAME |
| System Property       | openapi_validation_validator_name |
| Transformer Parameter | N/A                               |

**Default**: *atlassian*

At the moment, the extension supports only one validator - the one with name "atlassian". It uses [swagger-request-validator-core](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-core/) to load and validate request and response against OpenAPI files. If/when other validators are added, this setting will allow to switch between validator implementations.

The extension provides one additional validation to request and response: it requires content-type header to be present in request (if body is required) and in response (if response content exists).

### Failure Status Code

| Where to Set          | Name                                   |
|-----------------------|----------------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_FAILURE_STATUS_CODE |
| System Property       | openapi_validation_failure_status_code |
| Transformer Parameter | openapiValidationFailureStatusCode     |

**Default**: *500*

HTTP status code that is returned in case of validation failure can be customized using this setting.

### Ignore Errors

| Where to Set          | Name                             | Example                            |
|-----------------------|----------------------------------|------------------------------------|
| Environment Variable  | OPENAPI_VALIDATION_IGNORE_ERRORS | error1,error2                      |
| System Property       | openapi_validation_ignore_errors | error1,error2                      |
| Transformer Parameter | openapiValidationIgnoreErrors    | { "error1": true, "error2": true } |

**Default**:

This setting allows configuring which validation errors should be ignored, i.e., not fail the validation. Errors configured using environment variable or system property are ignored for all requests unless overridden by transformer parameters.

Environment variable and system property have form of comma-separated list of error keys to ignore. Transformer parameters is a map with error key as a key and boolean as a value - true to ignore the error, false to not ignore.

The error keys are validator-specific (please, refer to [validator name](#validator-name) for more info). For example, for "atlassian" validator you can check [its documentation](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-core/#markdown-header-controlling-validation-behaviour) for a list of possible errors.

There is a simple way how to find out key of error to ignore: response contains key for each error enclosed in `<i>` tag and in square brackets `[]`.

Example of validation failure response:

```html
<h1>Validation against OpenAPI failed</h1>
<h2>Request Errors</h2>
<ul>
	<li><i>[validation.request.body.missing]</i> A request body is required but none found.</li>
</ul>
<h2>Response Errors</h2>
<ul>
	<li><i>[validation.response.body.schema.required]</i> Object has missing required properties (["name","photoUrls"])</li>
</ul>
```

In the example above the request failed because of error with key `validation.request.body.missing` and the response has error with key `validation.response.body.schema.required`.

### Examples

Following code snippets show different examples of providing configuration. They are expected to be run on Linux or WSL. For Windows cmd or powershell, I believe, some similar approaches exist.

Provide configuration when running using downloaded jar files:

```bash
# use Java system properties
java -cp "wiremock-openapi-validation-extension-1.1.0.jar:wiremock-standalone-3.10.0.jar" \
  -Dopenapi_validation_file_path=https://petstore3.swagger.io/api/v3/openapi.json \
  -Dopenapi_validation_failure_status_code=418 \
  wiremock.Run
# use environment variables
OPENAPI_VALIDATION_FILE_PATH=https://petstore3.swagger.io/api/v3/openapi.json \
  OPENAPI_VALIDATION_FAILURE_STATUS_CODE=418 \
  java -cp "wiremock-openapi-validation-extension-1.1.0.jar:wiremock-standalone-3.10.0.jar" \
  wiremock.Run
```

Override parameters on per-stub level:

```json5
{
  "mappings": [
    {
      "request": {
        "urlPathTemplate": "/pet",
        "method": "POST"
      },
      "response": {
        "status": 415,
        "transformerParameters": {
          "openapiValidationFailureStatusCode": 599,
          "openapiValidationIgnoreErrors": {
            // during validation of this response unknown status won't be a problem
            "validation.response.status.unknown": true,
            // but missing request body will trigger validation failure
            // even if it is ignored using environment variable or system property
            "validation.request.body.missing": false
          }
        }
      }
    }
  ]
}
```

## Working with Source

For information on how to build, run and so on from source, refer to [BUILDING.md](./BUILDING.md).
