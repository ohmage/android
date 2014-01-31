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
import org.ohmage.models.AccessToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Requests an access_token from the server for a given user
 * TODO:check for authtoken error from the server and invalidate the google auth token
 */
public class AccessTokenRequest extends GsonRequest<AccessToken> {


    private Map<String, String> mParams = new HashMap<String, String>();

    /**
     * Make a request for an access_token from the server
     *
     * @param grantType
     * @param accessToken
     */
    public AccessTokenRequest(AuthUtil.GrantType grantType, String accessToken) {
        super(Endpoints.ACCESS_TOKEN, AccessToken.class, null);

        mParams.put("provider", grantType.getName());
        mParams.put("access_token", accessToken);
    }

    public AccessTokenRequest(String email, String password) {
        super(Endpoints.ACCESS_TOKEN, AccessToken.class, null);

        mParams.put("email", email);
        mParams.put("password", password);
    }

    public AccessTokenRequest(String refreshToken) {
        super(Endpoints.ACCESS_TOKEN, AccessToken.class, null);

        mParams.put("refresh_token", refreshToken);
    }

    @Override
    public Map<String, String> getParams() {
        return mParams;
    }
}
