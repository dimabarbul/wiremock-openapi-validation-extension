# Release

Version follow [semantic versioning](https://semver.org/), minor version is incremented after each update of WireMock.

The project uses [release plugin](https://maven.apache.org/maven-release/maven-release-plugin/) for performing release. Apart from changes in source control management, release pushes docker images using `docker push` and uploads build artifact to Sonatype.

**Before doing release** make sure [README.md](./README.md) and [Docker.md](./Docker.md) are updated with new version.

Note 1: release does not wait for artifact to be validated, only to be uploaded. Note 2: artifact pushed to Sonatype is not published, publishing is left to be done manually.

To make a release, run following commands:

```bash
# -Dstyle.color=always forces colored output despite batch mode (-B)
mvn -B -Dstyle.color=always release:prepare
mvn -B -Dstyle.color=always release:perform

# if something goes wrong during preparation, you can clean it up
mvn release:clean
```

Release can be tested in dry-run mode using following commands:

```bash
mvn -B -Dstyle.color=always release:prepare -DdryRun
mvn -B -Dstyle.color=always release:perform -DdryRun
```

Batch mode allows avoiding questions and makes release use preconfigured settings.

If you want to change connection to source control management system during deployment, you can provide custom SCM connection during `prepare` goal:

```bash
# by default, connection uses https, this example switches to git protocol
mvn -B -Dstyle.color=always release:prepare -Ddeveloper.connection=scm:git:git@github.com:dimabarbul/wiremock-openapi-validation-extension.git
# no need to provide it during perform as it's remembered in release.properties file
mvn -B -Dstyle.color=always release:perform
```

Release plugin is configured to advance minor version number (e.g., version 1.0.0-SNAPSHOT will become 1.0.0 and next one will be 1.1.0-SNAPSHOT). If you want to change versions, you can configure it during `prepare` goal:

```bash
mvn -B -Dstyle.color=always release:prepare -DreleaseVersion=1.0.1 -DdevelopmentVersion=1.1.0-SNAPSHOT
```

If you don't want to push docker images, you can add argument `-DskipDocker` to the commands above. This will disable Maven profile responsible for interacting with docker:

```bash
mvn -B -Dstyle.color=always release:prepare -Darguments='-DskipDocker'
mvn -B -Dstyle.color=always release:perform -Darguments='-DskipDocker'
```

### Manual Steps

After release there is a couple of manual steps:

- publish artifact on [Sonatype](https://central.sonatype.com/publishing) to make it available through Maven central repository
- update repository description on hub.docker.com (can just copy [Docker.md](./Docker.md))
- create release on GitHub

