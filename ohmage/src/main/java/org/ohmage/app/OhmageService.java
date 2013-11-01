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

import org.apache.http.auth.AuthenticationException;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.AuthUtil.GrantType;
import org.ohmage.models.AccessToken;
import org.ohmage.models.Ohmlets;
import org.ohmage.models.User;
import org.ohmage.sync.StreamWriterOutput;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by cketcham on 12/9/13.
 */
public interface OhmageService {

    @GET("/auth_token") AccessToken getAccessToken(@Query("refresh_token") String refreshToken)
            throws AuthenticationException;

    @GET("/auth_token") AccessToken getAccessToken(@Query("provider") GrantType provider,
            @Query("access_token") String token) throws AuthenticationException;

    @GET("/auth_token") void getAccessToken(@Query("provider") AuthUtil.GrantType grantType,
            @Query("access_token") String accessToken, CancelableCallback<AccessToken> callback);

    @GET("/auth_token")
    AccessToken getAccessToken(@Query("email") String email, @Query("password") String password)
            throws AuthenticationException;

    @GET("/auth_token")
    void getAccessToken(@Query("email") String email, @Query("password") String password,
            CancelableCallback<AccessToken> callback);

    @POST("/people") void createUser(@Query("provider") AuthUtil.GrantType grantType,
            @Query("access_token") String accessToken, @Body User user,
            CancelableCallback<User> callback);

    @POST("/people") void createUser(@Query("password") String password, @Body User user,
            CancelableCallback<User> callback);

    @GET("/ohmlets")
    void searchOhmlets(@Query("query") String query, Callback<Ohmlets> ohmletsCallback);

    @GET("/ohmlets") void getOhmlets(Callback<Ohmlets> ohmletsCallback);

    @POST("/streams/{streamId}/{streamVersion}/data")
    Response uploadStreamData(@Path("streamId") String streamId,
            @Path("streamVersion") String streamVersion, @Body StreamWriterOutput data)
            throws AuthenticationException;

    public abstract static class CancelableCallback<T> implements Callback<T> {

        private boolean mCancelled;

        public void cancel() {
            mCancelled = true;
        }

        public boolean isCancelled() {
            return mCancelled;
        }
    }
}