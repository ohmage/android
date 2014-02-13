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
public class OhmageContract {

    public static final String CONTENT_AUTHORITY = "org.ohmage.db";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface OhmletColumns {
        /**
         * Unique string identifying the stream
         */
        String OHMLET_ID = "ohmlet_id";

        /**
         * Version to identify stream
         */
        String OHMLET_VERSION = "ohmlet_version";

        /**
         * String array of surveys for this ohmlet *
         */
        String OHMLET_SURVEYS = "ohmlet_surveys";

        /**
         * String array of streams for this ohmlet *
         */
        String OHMLET_STREAMS = "ohmlet_streams";

        /**
         * The privacy state for this ohmlet *
         */
        String OHMLET_PRIVACY_STATE = "ohmlet_privacy_state";

        /**
         * Join state for this ohmlet *
         */
        String OHMLET_JOIN_STATE = "ohmlet_join_state";
    }

    private static final String PATH_OHMLETS = "ohmlets";

    /**
     * Represents an ohmlet.
     */
    public static final class Ohmlets implements BaseColumns, OhmletColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_OHMLETS)
                                                              .build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.ohmage.ohmlets.ohmlet";

        public static enum JoinState {
            NONE,
            INVITED,
            REQUESTED,
            JOINED,
        }

        public static enum PrivacyState {
            PRIVATE,
            INVITE_ONLY,
            PUBLIC,
        }
    }
}
