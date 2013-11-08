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

package org.ohmage.requests;


import org.ohmage.app.Endpoints;
import org.ohmage.models.Streams;

/**
 * Uploads stream data
 */
public class UploadStreamDataRequest extends GsonRequest<Streams> {

    /**
     * Make a request to upload stream data
     */
    public UploadStreamDataRequest(String streamId, String streamVersion, Streams streams) {
        super(buildUrl(streamId, streamVersion), Streams.class, streams);
    }

    /**
     * Builds the Url with the parameters. Handles the two types of authentication
     * we currently support.
     *
     * @param streamId
     * @param streamVersion
     * @return the url with the specified parameters
     */
    private static String buildUrl(String streamId, String streamVersion) {
        StringBuilder url = new StringBuilder(Endpoints.STREAMS);
        return url.append(streamId).append("/").append(streamVersion).toString();
    }
}
