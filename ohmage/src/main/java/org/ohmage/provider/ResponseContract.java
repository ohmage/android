/*
 * Copyright (C) 2014 ohmage
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

package org.ohmage.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by cketcham on 1/29/14.
 */
public class ResponseContract {

    public static final String CONTENT_AUTHORITY = "org.ohmage.responses";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface ResponseColumns {
        /**
         * Unique string identifying the survey
         */
        String SURVEY_ID = "survey_id";

        /**
         * Version to identify survey
         */
        String SURVEY_VERSION = "survey_version";

        /**
         * Metadata of the response
         */
        String RESPONSE_METADATA = "response_metadata";

        /**
         * Extra data for the response such as where files exist on the system
         * keyed by the prompt ids
         */
        String RESPONSE_EXTRAS = "response_extras";

        /**
         * Data of the response
         */
        String RESPONSE_DATA = "response_data";
    }

    private static final String PATH_RESPONSES = "responses";

    /**
     * Represents a response.
     */
    public static final class Responses implements BaseColumns, ResponseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_RESPONSES)
                        .build();

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.dir/vnd.ohmage.responses.response";

    }
}
