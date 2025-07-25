/*
 * Copyright 2025 Dmitriy Barbul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.dimabarbul.wiremock.openapi_validation;

import java.util.Optional;

class SystemAccessorImpl implements SystemAccessor {
    @Override
    public Optional<String> getEnvironmentVariable(final ValidationParameter parameter) {
        return Optional.ofNullable(System.getenv(parameter.envName()));
    }

    @Override
    public Optional<String> getSystemProperty(final ValidationParameter parameter) {
        return Optional.ofNullable(System.getProperty(parameter.systemPropertyName()));
    }
}
