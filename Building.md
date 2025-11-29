# Building

The document describes how to build, run, debug, release the project and so on.

## Build and Package

To build the project you need Maven 3.8+ and JDK 11 installed. To build project just run:

```bash
mvn clean compile
```

The project uses [reproducible build](https://maven.apache.org/guides/mini/guide-reproducible-builds.html).

This will compile the project and allow running it using `mvn exec:java` command shown later in [run](#run) section. Building using the command above runs all necessary checks including checking code style using [spotless](https://github.com/diffplug/spotless) with [palantir format](https://github.com/diffplug/spotless/tree/main/plugin-maven#palantir-java-format) and [license headers](https://oss.carbou.me/license-maven-plugin). If you want to skip those two checks, you can run command:

```bash
mvn clean compile -DskipCodeStyle
```

To generate jar file (result will be `target/wiremock-openapi-validation-extension-<VERSION>.jar`) and build docker images:

```bash
mvn clean package
```

## Docker Images

Docker images will have tags based on project version. For example, for version x.y.z images will have following tags:

- for version ending with `-SNAPSHOT`:
  - x.y.z-snapshot
  - snapshot
  - x.y.z-snapshot-alpine
  - snapshot-alpine
- for version not ending with `-SNAPSHOT`:
  - x.y.z
  - x.y
  - latest
  - x.y.z-alpine
  - x.y-alpine
  - alpine

To build docker images there are two options: after generating jar file run `build-images.sh` or run Maven phase `package`:

```bash
# 1st option
# create JAR file without building docker image
mvn clean package -DskipDocker
# make sure JAR file has been created before, i.e., this command will build
# docker image with JAR file from last run of `mvn package`
./build-images.sh

# 2nd option
mvn clean package
```

`build-images.sh` script gives more flexibility when building images. It uses following environment variables:

| Environment Variable | Description                                                                        |
|----------------------|------------------------------------------------------------------------------------|
| PUSH                 | If not empty, docker images will be pushed.                                        |
| NOBUILD              | If not empty, docker images will NOT be built.                                     |
| PROJECT_VERSION      | If not empty, its value will be used, otherwise it will be calculated using Maven. |

## Deploy

Deploy phase pushes docker images (using `docker push` by running `build-images.sh` script with environment variables NOBUILD=1 and PUSH=1) and deploys artifacts to Sonatype (using `org.sonatype.central:central-publishing-maven-plugin` as described [here](https://central.sonatype.org/publish/publish-portal-maven/)) with server ID `ossrh`.

You can copy `settings-template.xml` to your local Maven `settings.xml` location (usually found at `~/.m2/settings.xml` on Unix-based systems or `%USERPROFILE%\.m2\settings.xml` on Windows). Update the template with your credentials.

Pushing docker images may be skipped using system property `skipDocker`:

```bash
mvn clean deploy -DskipDocker
```

Publishing artifact requires signing using GPG (refer to [Sonatype guide](https://central.sonatype.org/publish/requirements/gpg/) and [Maven GPG plugin doc](https://maven.apache.org/plugins/maven-gpg-plugin/) for more info).

To skip publishing artifact to Sonatype you can use system property `skipSonatype`:

```bash
mvn clean deploy -DskipSonatype
```

Check [documentation](https://central.sonatype.org/publish/publish-portal-maven/#credentials) for more information.

## Run

### Using Exec Plugin

You can run the project using [exec plugin](https://www.mojohaus.org/exec-maven-plugin/). This requires the project to be compiled (`mvn compile`) beforehand, otherwise WireMock server won't see the extension.

Execute one of the following commands in root repository directory:

```bash
# run with default configuration
# this requires OpenAPI file to exist in repository root
# if you need mappings, they should also exist in repository root
mvn exec:java -Dexec.mainClass=wiremock.Run

# use custom OpenAPI file and print the extension config on start
mvn exec:java -Dexec.mainClass=wiremock.Run \
    -Dopenapi_validation_file_path=demo/openapi.json \
    -Dopenapi_validation_print_config=true

# the same as above, but instructs WireMock to use mappings from demo/mappings
mvn exec:java -Dexec.mainClass=wiremock.Run \
    -Dexec.args='--root-dir=demo' \
    -Dopenapi_validation_file_path=demo/openapi.json \
    -Dopenapi_validation_print_config=true

# you can pass the extension configuration using environment variables
OPENAPI_VALIDATION_FILE_PATH=demo/openapi.json \
    OPENAPI_VALIDATION_PRINT_CONFIG=true \
    mvn exec:java -Dexec.mainClass=wiremock.Run \
    -Dexec.args='--root-dir=demo'

# use mvnDebug to be able to attach to the process
OPENAPI_VALIDATION_FILE_PATH=demo/openapi.json \
    OPENAPI_VALIDATION_PRINT_CONFIG=true \
    mvnDebug exec:java -Dexec.mainClass=wiremock.Run \
    -Dexec.args='--root-dir=demo'
```

### Using WireMock Standalone Jar

You can download WireMock standalone jar file and run the project jar file alongside it.

```bash
wget https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.13.2/wiremock-standalone-3.13.2.jar
mvn clean package
java -cp "target/wiremock-openapi-validation-extension-<VERSION>-jar-with-dependencies.jar:wiremock-standalone-3.13.2.jar" wiremock.Run

# the same but allowing remote debug
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -cp "target/wiremock-openapi-validation-extension-<VERSION>-jar-with-dependencies.jar:wiremock-standalone-3.13.2.jar" wiremock.Run
```

## Code Style

The project uses [palantir format](https://github.com/palantir/palantir-java-format). It can be integrated into IntellijIdea using [palantir-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/13180) - install it, enable in settings and use your preferred way to format code in the IDE.

Spotless plugin that is used to check format allows fixing format:

```bash
mvn spotless:apply
```

License header must be present in almost all files. License plugin is used to verify this. It also allows automatic fix:

```bash
mvn license:format
```

## Branching Strategy

The project uses a simple trunk-based development model with a single `master` branch. All development work and releases
happen directly on `master`. Releases are tagged using Git tags to mark specific versions.

When contributing:

1. Create feature branches from `master` if needed for larger changes.
2. Keep feature branches short-lived and merge back to `master` quickly.

Git tags mark release, they are created by Maven release plugin. Refer to [Release](Release.md) document for more details on the release.
