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
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.User;

/**
 * Creates a user on the server
 * TODO:check for authtoken error from the server and invalidate the google auth token
 */
public class CreateUserRequest extends GsonRequest<User> {

    /**
     * Make a request for an access_token from the server
     *
     * @param grantType
     * @param accessToken
     */
    public CreateUserRequest(AuthUtil.GrantType grantType, String accessToken, User user) {
        super(buildCreateUserUrl(grantType, accessToken), User.class, user);
    }

    /**
     * Builds the Url with the parameters. Handles the two types of authentication
     * we currently support.
     *
     * @param grantType
     * @param accessToken
     * @return the url with the specified parameters
     */
    private static String buildCreateUserUrl(AuthUtil.GrantType grantType, String accessToken) {
        StringBuilder url = new StringBuilder(Endpoints.USERS);
        switch (grantType) {
            case GOOGLE_OAUTH2:
                url.append("?provider=").append(grantType.getName())
                   .append("&access_token=").append(accessToken);
                break;
            case CLIENT_CREDENTIALS:
                url.append("?password=").append(accessToken);
                break;
        }
        return url.toString();
    }
}