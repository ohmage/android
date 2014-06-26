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
import org.ohmage.models.OAuthClient;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Ohmlets;
import org.ohmage.models.Stream;
import org.ohmage.models.Survey;
import org.ohmage.models.Surveys;
import org.ohmage.models.User;
import org.ohmage.sync.ResponseTypedOutput;
import org.ohmage.sync.StreamWriterOutput;

import java.util.Collection;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

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

    //This path is checked in the OkClient to prevent redirects
    @GET("/oauth/authorize?response_type=code")
    Observable<String> OAuthAuthorize(@Query("client_id") String clientId, @Query("scope") String scope,
            @Query("state") String state);

    //This path is checked in the OkClient to prevent redirects
    @POST("/oauth/authorization_with_token")
    Observable<String> OAuthAuthorized(@Query("granted") boolean granted, @Query("code") String code);

    @GET("/oauth/clients/{client_id}")
    Observable<OAuthClient> OAuthClientInfo(@Path("client_id") String clientId);

    @POST("/people") void createUser(@Query("provider") AuthUtil.GrantType grantType,
            @Query("access_token") String accessToken, @Body User user,
            CancelableCallback<User> callback);

    @POST("/people") void createUser(@Query("password") String password, @Body User user,
            CancelableCallback<User> callback);

    @POST("/people") void createUser(@Query("password") String password, @Body User user,
            @Query("user_invitation_id") String inviteCode, CancelableCallback<User> callback);

    @GET("/people/{userId}/current/")
    Observable<User> getCurrentStateForUser(@Path("userId") String userId)
            throws AuthenticationException;

    @GET("/ohmlets")
    void searchOhmlets(@Query("query") String query, Callback<Ohmlets> ohmletsCallback);

    @GET("/ohmlets") void getOhmlets(Callback<Ohmlets> ohmletsCallback);

    @GET("/ohmlets/{ohmletId}") Observable<Ohmlet> getOhmlet(@Path("ohmletId") String id);

    @POST("/ohmlets/{ohmletId}/people")
    Response updateMemberForOhmlet(@Path("ohmletId") String ohmletId, @Body Member member)
            throws AuthenticationException;

    @POST("/ohmlets/{ohmletId}/people")
    Response updateMemberForOhmlet(@Path("ohmletId") String ohmletId, @Body Member member,
            @Query("ohmlet_invitation_id") String inviteCode) throws AuthenticationException;

    @DELETE("/ohmlets/{ohmletId}/people/{userId}")
    Response removeUserFromOhmlet(@Path("ohmletId") String ohmletId, @Path("userId") String userId)
            throws AuthenticationException;

    @GET("/surveys") void getSurveys(Callback<Surveys> surveysCallback);

    @GET("/surveys/{surveyId}/{surveyVersion}")
    Observable<Survey> getSurvey(@Path("surveyId") String surveyId,
            @Path("surveyVersion") long surveyVersion);

    @POST("/surveys/{surveyId}/{surveyVersion}/data")
    Observable<Response> uploadResponse(@Path("surveyId") String surveyId,
            @Path("surveyVersion") long surveyVersion, @Body ResponseTypedOutput data)
            throws AuthenticationException;

    @GET("/streams/{streamId}")
    Observable<Collection<Integer>> getStreamVersions(@Path("streamId") String streamId);

    @GET("/streams/{streamId}/{streamVersion}")
    Observable<Stream> getStream(@Path("streamId") String streamId,
            @Path("streamVersion") long streamVersion);

    @POST("/streams/{streamId}/{streamVersion}/data")
    Response uploadStreamData(@Path("streamId") String streamId,
            @Path("streamVersion") long streamVersion, @Body StreamWriterOutput data)
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