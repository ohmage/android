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
import org.ohmage.sync.StreamWriterOutput;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by cketcham on 12/9/13.
 */
public interface OhmageService {
    @POST("/streams/{id}/{version}/data")
    Response uploadStreamData(@Header("Authorization") String token, @Path("id") String streamId,
                              @Path("version") String streamVersion,
                              @Body StreamWriterOutput data)
            throws AuthenticationException;
//    Response uploadStreamData(Account account, @Path("id") String streamId,
//                              @Path("version") String streamVersion,
//                              @Body StreamSyncAdapter.ProbeWriterBody data)
//            throws AuthenticationException;
}