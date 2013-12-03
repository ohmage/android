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

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.squareup.otto.Bus;

import org.ohmage.app.Ohmage;

import java.util.Arrays;

import javax.inject.Inject;

import dagger.Lazy;

/**
 * This base request posts errors that might occur to the otto bus.
 */
public abstract class OttoRequest<T> extends Request<T> {

    private static final String TAG = OttoRequest.class.getSimpleName();

    @Inject Lazy<Bus> bus;

    private Response.Listener<T> mResponseListener;

    private Response.ErrorListener mErrorListener;

    public OttoRequest(int method, String url) {
        super(method, url, null);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    public void setResponseListener(Response.Listener<T> listener) {
        mResponseListener = listener;
    }

    public Response.Listener<T> getResponseListener() {
        return mResponseListener;
    }

    public void setErrorListener(Response.ErrorListener listener) {
        mErrorListener = listener;
    }

    public Response.ErrorListener getErrorListener() {
        return mErrorListener;
    }

    @Override
    public void deliverResponse(T response) {
        if (mResponseListener != null)
            mResponseListener.onResponse(response);
        bus.get().post(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (mErrorListener != null)
            mErrorListener.onErrorResponse(error);
        if (error instanceof ServerError && error.networkResponse.statusCode == 412) {
            bus.get().post(new NoAccountEvent(error));
        } else {
            if (error.getCause() != null)
                error.getCause().printStackTrace();
            bus.get().post(error);
        }

        if (error.networkResponse != null && error.networkResponse.data != null) {
            Log.d(TAG, new String(error.networkResponse.data));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Request) {
            Request request = (Request) other;
            try {
                return request.getUrl().equals(getUrl())
                        && request.getMethod() == getMethod()
                        && Arrays.equals(request.getBody(), getBody());
            } catch (AuthFailureError authFailureError) {
                authFailureError.printStackTrace();
            }
        }
        return false;
    }

    public class NoAccountEvent {
        VolleyError error;

        public NoAccountEvent(VolleyError error) {
            this.error = error;
        }
    }
}
