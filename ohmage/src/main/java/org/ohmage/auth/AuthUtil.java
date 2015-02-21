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

package org.ohmage.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.common.Scopes;

import org.ohmage.app.Ohmage;
import org.ohmage.provider.OhmageContract;

/**
 * Utilities for dealing with Authentication providers and local accounts
 */
public class AuthUtil {

    /**
     * Account type string.
     */
    public static final String ACCOUNT_TYPE = "org.ohmage.ACCOUNT";

    /**
     * Authtoken type string.
     */
    public static final String AUTHTOKEN_TYPE = "org.ohmage.ALL";

    // Sync interval constants
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SYNC_INTERVAL_IN_MINUTES = 60L;
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    public static final String OMH_CLIENT_ID = "org.openmhealth.android.ohmage";
    public static final String OMH_AUTH_HEADER = "Authorization: Basic b3JnLm9wZW5taGVhbHRoLmFuZHJvaWQub2htYWdlOjVFbzQzamtMRDd6NzZj";

    public static final class Google {
        public static final String[] SCOPES = {
                "email",
                "https://www.googleapis.com/auth/userinfo.profile"
        };

        public static final String ACCESS_TOKEN_SCOPE = "oauth2:" + TextUtils.join(" ", SCOPES);

        public static final String CODE_SCOPE = "oauth2:server:client_id:48636836762-mulldgpmet2r4s3f16s931ea9crcc64m.apps.googleusercontent.com:api_scope:" + TextUtils.join(" ", SCOPES);
//        public static final String CODE_SCOPE = "oauth2:server:client_id:494798802772-2jom9to52beivt0vb1g4o0fhejgqsnko.apps.googleusercontent.com:api_scope:" + TextUtils.join(" ", SCOPES);
    }

    /**
     * GrantType options
     */
    public static enum GrantType {
        /**
         * Authorize with google
         */
        GOOGLE_OAUTH2("google"),

        /**
         * Authorize with ohmage account
         */
        CLIENT_CREDENTIALS("client-credentials");

        private String type;

        GrantType(String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    public static void requestSync() {
        AccountManager am = AccountManager.get(Ohmage.app());
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length > 0) {
            Ohmage.app().getContentResolver()
                    .requestSync(accounts[0], OhmageContract.CONTENT_AUTHORITY, new Bundle());
        }
    }
}