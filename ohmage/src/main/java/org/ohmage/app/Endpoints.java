/*
 * Copyright (C) 2013 ohmage
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

package org.ohmage.app;

/**
 * The ohmage API endpoints.
 */
public class Endpoints {

    /**
     * The UserAgent string supplied with all ohmage HTTP requests.
     */
    public static final String USER_AGENT = "ohmage Android Agent";

    /**
     * The protocol and hostname used to access the ohmage service.
     */
    public static final String API_HOST = "https://dev.ohmage.org";

    /**
     * The URL root used to access the ohmage API.
     */
    public static final String API_ROOT = API_HOST + "/ohmage";

    /**
     * The API URL used to get an access_token from ohmage.
     */
    public static final String ACCESS_TOKEN = API_ROOT + "/auth_token";

    /**
     * The API URL used to access ohmage users.
     */
    public static final String USERS = API_ROOT + "/users";

    /**
     * The API URL used to access streams.
     */
    public static final String STREAMS = API_ROOT + "/streams";

    public static final class Stream {
        public static final String stream(org.ohmage.models.Stream stream) {
            return Endpoints.STREAMS + "/" + stream.id + "/" + stream.version;
        }

        public static final String data(org.ohmage.models.Stream stream) {
            return stream(stream) + "/data";
        }
    }
}