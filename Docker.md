# WireMock with OpenAPI Validation

The images provide standalone WireMock server with extension to validate request and/or response against OpenAPI file.

The images are based on [wiremock/wiremock](https://hub.docker.com/r/wiremock/wiremock) and just provide one additional jar file. This means that you can use all capabilities that `wiremock/wiremock` provides, for example, health checks, provide any additional arguments in WIREMOCK_OPTIONS environment variable or in CLI argument and so on.

## Quick Start

To quickly get it up and running you can use demo OpenAPI and mapping files that are available in the [source code](https://github.com/dimabarbul/wiremock-openapi-validation-extension/tree/master/demo). Go to temporary folder and execute following:

```bash
# clone source code
git clone https://github.com/dimabarbul/wiremock-openapi-validation-extension.git
# run alpine image providing mapping and OpenAPI files from "demo" folder
docker run -it --rm -p 8080:8080 \
    -v $PWD/wiremock-openapi-validation-extension/demo:/home/wiremock \
    dimabarbul/wiremock-openapi-validation:alpine \
    --verbose
```

You'll see standard WireMock greeting with one difference - you'll see extension "openapi-validation" loaded.

### Play with It

#### Schema

Schema of user returned from API is following:

```json
{
  "type": "object",
  "properties": {
    "id": {"type": "integer"},
    "externalId": {"type": "string", "format": "uuid"},
    "username": {"type": "string", "minLength": 2},
    "role": {
      "type": "string",
      "enum": ["user", "admin"]
    }
  },
  "required": ["id", "username", "role"]
}
```

Schema of request body when adding user is different only in that it does not allow passing `id`.

#### Scenarios

Mapping for user with ID 1 is valid. Get user with ID 1:

```bash
curl http://localhost:8080/users/1
```

You'll get the response:

```json
{
  "id": 1,
  "username": "user1",
  "role": "user"
}
```

Mapping for user with ID 2 is invalid according to OpenAPI file. Try to get user with ID 2:

```bash
curl http://localhost:8080/users/2
```

The response that was about to be returned is (you can find it in WireMock output):

```json
{
  "id": 2,
  "externalId": "user2",
  "username": "user2",
  "role": "unknown"
}
```

You'll get response saying what exactly is wrong:

```html
<h1>Validation against OpenAPI failed</h1>
<h2>Request Errors</h2>
<b>No errors</b>
<h2>Response Errors</h2>
<ul>
	<li><i>[validation.response.body.schema.format.uuid]</i> [Path '/externalId'] Input string "user2" is not a valid UUID</li>
	<li><i>[validation.response.body.schema.enum]</i> [Path '/role'] Instance value ("unknown") not found in enum (possible values: ["user","admin"])</li>
</ul>
```

If you try to add user without providing anything:

```bash
curl http://localhost:8080/users -X POST
```

, you'll see that it's invalid:

```html
<h1>Validation against OpenAPI failed</h1>
<h2>Request Errors</h2>
<ul>
	<li><i>[validation.request.body.missing]</i> A request body is required but none found.</li>
	<li><i>[validation.request.contentType.missing]</i> Request Content-Type header is missing</li>
</ul>
<h2>Response Errors</h2>
<ul>
	<li><i>[validation.response.body.schema.minLength]</i> [Path '/username'] String "" is too short (length: 0, required minimum: 2)</li>
</ul>
```

As you can see, there is an error in response as well, that's because mapping leverages [response templating](https://wiremock.org/docs/response-templating/) feature and uses request body parts in response. The response it was about to return was (mapping uses randomInt for `id`, so it might be different):

```json
{
  "id": 8,
  "role": "user",
  "username": ""
}
```

Let's correct the issues in the request and see success:

```bash
curl http://localhost:8080/users \
    -d '{"role":"user", "username":"xx"}' \
    -H 'Content-type: application/json; charset=utf-8'
```

Now you should see response from mapping (mapping uses randomInt for `id`, so it might be different):

```json
{
  "id": 4,
  "role": "user",
  "username": "xx"
}
```

## Tags

Following image tags are supported:

| Tag                                  | Based on WireMock image tag |
|--------------------------------------|-----------------------------|
| 1.4.0<br>1.4<br>latest               | 3.13.0-1                    |
| 1.4.0-alpine<br>1.4-alpine<br>alpine | 3.13.0-1-alpine             |

## Running

By default, the extension expects OpenAPI file to be `/home/wiremock/openapi.json`, `/home/wiremock/openapi.yaml` or `/home/wiremock/openapi.yml`. So running it like this:

```bash
docker run -it --rm \
  -p 8080:8080 \
  -v $PWD/openapi.json:/home/wiremock/openapi.json \
  -v $PWD/mappings:/home/wiremock/mappings \
  -v $PWD/__files:/home/wiremock/__files \
  dimabarbul/wiremock-openapi-validation:latest
```

will validate requests and responses against `openapi.json` file in current folder where you run the command. OpenAPI file can also be in YAML format.

Another option is to provide URL as OpenAPI file:

```bash
docker run -it --rm \
  -p 8080:8080 \
  -e OPENAPI_VALIDATION_FILE_PATH=https://petstore3.swagger.io/api/v3/openapi.json \
  -v $PWD/mappings:/home/wiremock/mappings \
  -v $PWD/__files:/home/wiremock/__files \
  dimabarbul/wiremock-openapi-validation:latest
```

Configuration can be passed in environment variables (as shown above) or using Java system properties via JAVA_OPTS environment variable:

```bash
docker run -it --rm \
  -p 8080:8080 \
  -e JAVA_OPTS="-Dopenapi_validation_file_path=https://petstore3.swagger.io/api/v3/openapi.json" \
  -v $PWD/mappings:/home/wiremock/mappings \
  -v $PWD/__files:/home/wiremock/__files \
  dimabarbul/wiremock-openapi-validation:latest
```

For more information about configuration, please, refer to [Configuration](https://github.com/dimabarbul/wiremock-openapi-validation-extension#configuration) section in README.

Any arguments that `wiremock/wiremock` supports can be passed. Example:

```bash
docker run -it --rm \
  -p 8080:8080 \
  -v $PWD/openapi.json:/home/wiremock/openapi.json \
  -v $PWD/mappings:/home/wiremock/mappings \
  -v $PWD/__files:/home/wiremock/__files \
  dimabarbul/wiremock-openapi-validation:latest \
  --verbose \
  --global-response-templating
```
