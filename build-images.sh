#!/bin/bash

set -e

if [ "$1" = "--help" -o "$1" = "-h" ]; then
    echo "Just execute the script"
    echo "Environment variables:"
    echo "  NOBUILD - empty to build docker images, non-empty to skip build"
    echo "  PUSH    - non-empty to push docker images to docker registry"
    echo "            empty to skip push"
    exit 1;
fi

echo "Calculating project version"
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
IMAGE_NAME=dimabarbul/wiremock-openapi-validation
IMAGE_VERSION=$(tr '[:upper:]' '[:lower:]' <<<${PROJECT_VERSION})
IMAGE_VERSION_ALPINE=${IMAGE_VERSION}-alpine
if [ -z "${PUSH}" ]; then
    PUSH=false;
else
    PUSH=true;
fi
if [ -z "${NOBUILD}" ]; then
    BUILD=true;
else
    BUILD=false;
fi

echo
echo "Project version:         ${PROJECT_VERSION}"
echo "Image name:              ${IMAGE_NAME}"
echo "Image version:           ${IMAGE_VERSION}"
echo "Image alpine version:    ${IMAGE_VERSION_ALPINE}"
echo "Push:                    ${PUSH}"
echo

if [ ! -f target/wiremock-openapi-validation-extension-${PROJECT_VERSION}.jar ]; then
    echo 'Run `mvn clean package` before building images.'
    exit 1;
fi

if [ "${BUILD}" = "true" ]; then
    echo "Building ${IMAGE_VERSION}"
    docker build -t ${IMAGE_NAME}:${IMAGE_VERSION} .
    echo "Building ${IMAGE_VERSION_ALPINE}"
    docker build -t ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE} -f Dockerfile-alpine .
fi

if [ "${PUSH}" = "true" ]; then
    echo "Pushing ${IMAGE_VERSION}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION}
    echo "Pushing ${IMAGE_VERSION_ALPINE}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE}
fi

