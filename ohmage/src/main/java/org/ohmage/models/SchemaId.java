/*
 * Copyright (C) 2015 ohmage
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

package org.ohmage.models;

/**
 * Created by cketcham on 2/21/15.
 */
public class SchemaId {
    private static final String DELIMITER = ":";

    String namespace;
    String name;
    Version version;

    public SchemaId() {}

    public SchemaId(String id, String version) {
        namespace = id.split(DELIMITER)[0];
        name = id.split(DELIMITER)[1];
        this.version = new Version(version);
    }

    @Override
    public String toString() {
        return namespace + DELIMITER + name;
    }

    public String getVersion() {
        return version.toString();
    }
}
