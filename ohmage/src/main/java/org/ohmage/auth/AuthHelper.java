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

import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.ohmage.app.MainActivity;

import java.io.IOException;

/**
 * Helper class for dealing with accounts and authentication
 */
public class AuthHelper {

    private final Context mContext;

    public AuthHelper(Context context) {
        mContext = context;
    }

    public String googleAuthGetToken(String googleAccount) throws UserRecoverableAuthException,
            IOException, GoogleAuthException {
        String code = GoogleAuthUtil.getToken(mContext, googleAccount, AuthUtil.Google.CODE_SCOPE, null);
        // clear it immediately to avoid stale code from being cached
        GoogleAuthUtil.clearToken(mContext, code);
        return "fromApp_" + code;
    }
}
