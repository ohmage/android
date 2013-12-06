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

import android.util.Log;

import org.apache.http.auth.AuthenticationException;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

/**
 * Created by cketcham on 12/9/13.
 */
public class OhmageErrorHandler implements ErrorHandler {

    private static final String TAG = OhmageErrorHandler.class.getSimpleName();

    @Override public Throwable handleError(RetrofitError cause) {
        Response r = cause.getResponse();

        if (r != null && r.getBody() instanceof TypedByteArray) {
            String body = new String(((TypedByteArray) r.getBody()).getBytes());
            Log.e(TAG, r.getStatus() + ": " + body);
        }

        if (r != null && r.getStatus() == 401) {
            return new AuthenticationException("Error authenticating with ohmage", cause);
        }
        return cause;
    }
}
