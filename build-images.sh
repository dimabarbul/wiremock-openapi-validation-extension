#!/bin/bash
#
# Copyright 2024 Dmitriy Barbul
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
if [[ "${PROJECT_VERSION}" =~ -SNAPSHOT$ ]]; then
    IS_SNAPSHOT_VERSION=true
else
    IS_SNAPSHOT_VERSION=false
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
    if [ "${IS_SNAPSHOT_VERSION}" = "false" ]; then
        echo "Tagging alpine image with tag latest"
        docker tag ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE} ${IMAGE_NAME}:latest
    fi
fi

if [ "${PUSH}" = "true" ]; then
    echo "Pushing ${IMAGE_VERSION}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION}
    echo "Pushing ${IMAGE_VERSION_ALPINE}"
    docker push ${IMAGE_NAME}:${IMAGE_VERSION_ALPINE}
    if [ "${IS_SNAPSHOT_VERSION}" = "false" ]; then
        echo "Pushing latest"
        docker push ${IMAGE_NAME}:latest
    fi
fi

