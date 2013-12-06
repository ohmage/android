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

package org.ohmage.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.Stream;
import org.ohmage.models.Streams;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.converter.ConversionException;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class StreamSyncAdapter extends AbstractThreadedSyncAdapter {

    @Inject AccountManager accountManager;

    @Inject OhmageService ohmageService;

    private static final String TAG = "StreamSyncAdapter";

    /**
     * If set, only points of this stream will be uploaded
     */
    public static final String EXTRA_STREAM_ID = "extra_stream_id";

    /**
     * If set, only points of this version of the stream will be uploaded. {@link #EXTRA_STREAM_ID}
     * must also be set for this parameter to be used
     */
    public static final String EXTRA_STREAM_VERSION = "extra_stream_version";

//    private Account mAccount;
//
//    private Streams mStreams;
//
//    private StreamWriterOutput mWriter;
//
//    private ContentProviderClient mProvider;

    /**
     * Set up the sync adapter
     */
    public StreamSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public StreamSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        String streamId = extras.getString(EXTRA_STREAM_ID);
        String streamVersion = extras.getString(EXTRA_STREAM_VERSION);

        performSyncForStreams(account, new Streams(provider, account, streamId, streamVersion),
                new StreamWriterOutput(provider), syncResult);
    }

    public void performSyncForStreams(Account account, Streams streams, StreamWriterOutput writer,
                                      SyncResult syncResult) {
        Log.d("ohmage", "start stream sync");

        try {
            for (Stream stream : streams) {
                try {
                    writer.query(account.name, stream);

                    while (writer.moveToNextBatch()) {
                        sendData(account, stream, writer);
                        syncResult.stats.numEntries += writer.deleteBatch();
                    }
                } catch (RetrofitError e) {
                    if (e.getCause() instanceof IOException)
                        syncResult.stats.numIoExceptions++;
                    else if (e.getCause() instanceof ConversionException) {
                        syncResult.stats.numParseExceptions++;
                    } else {
                        // Skipped entries for some other error
                        syncResult.stats.numSkippedEntries++;
                        e.printStackTrace();
                    }
                } finally {
                    writer.close();
                }
            }
        } catch (RemoteException e) {
            syncResult.databaseError = true;
        } catch (AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
        } finally {
            Log.d(TAG, "Finished Sync");
        }
    }

    /**
     * Streams points over the network to the ohmage server.
     *
     * @param account the account we are syncing
     * @param stream  the stream we are currently syncing
     * @param data    A reference to the data via a {@link DeletingCursor}
     * @throws AuthenticationException thrown if there is a problem getting the auth token or
     *                                 the server responds with an auth error
     * @throws RetrofitError           thrown if there is an error communicating with the server
     *                                 which causes us to stop syncing
     */
    private void sendData(Account account, Stream stream, StreamWriterOutput data) throws
            AuthenticationException, RetrofitError {
        sendData(account, stream, data, true);
    }

    private void sendData(Account account, Stream stream, StreamWriterOutput data,
                          boolean retryIfAuthError) throws AuthenticationException, RetrofitError {
        String token = null;
        try {
            token = accountManager.blockingGetAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, true);
        } catch (OperationCanceledException e) {
            throw new AuthenticationException("Error getting auth token.", e);
        } catch (AuthenticatorException e) {
            throw new AuthenticationException("Error getting auth token.", e);
        } catch (IOException e) {
            throw new AuthenticationException("Error getting auth token.", e);
        }

        if(token == null)
            throw new AuthenticationException("Token could not be retrieved.");

        try {
            ohmageService.uploadStreamData("ohmage " + token, stream.id, stream.version, data);
        } catch (AuthenticationException e) {
            // If the response failed because of an auth error, we will try one more time
            if (retryIfAuthError) {
                accountManager.invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, token);
                try {
                    sendData(account, stream, data, false);
                } catch (AuthenticationException e1) {
                    throw e1;
                }
            } else {
                throw e;
            }
        }
    }
}
