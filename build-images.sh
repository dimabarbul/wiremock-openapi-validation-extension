#!/bin/bash

set -e

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

echo
echo "Project version:         ${PROJECT_VERSION}"
echo "Image name:              ${IMAGE_NAME}"
echo "Image version:           ${IMAGE_VERSION}"
echo "Image alpine version:    ${IMAGE_VERSION_ALPINE}"
echo "Push:                    ${PUSH}"
echo

echo "Building ${IMAGE_VERSION}"
docker build -t ${IMAGE_NAME}:${IMAGE_VERSION} .
echo "Building ${IMAGE_VERSION_ALPINE}"
docker build -t ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE} -f Dockerfile-alpine .
echo "Tagging alpine latest"
docker tag ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE} ${IMAGE_NAME}:latest

if [ "${PUSH}" = "true" ]; then
    echo "Pushing ${IMAGE_VERSION}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION}
    echo "Pushing ${IMAGE_VERSION_ALPINE}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE}
    echo "Pushing latest"
    docker push ${IMAGE_NAME}:latest
fi

