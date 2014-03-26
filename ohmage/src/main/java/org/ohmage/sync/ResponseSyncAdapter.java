/*
 * Copyright (C) 2014 ohmage
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

import com.google.gson.Gson;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.provider.ResponseContract.Responses;
import org.ohmage.sync.ResponseTypedOutput.ResponseFiles;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class ResponseSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String IS_SYNCADAPTER = "is_syncadapter";

    @Inject AccountManager am;

    @Inject OhmageService ohmageService;

    @Inject Gson gson;

    private static final String TAG = ResponseSyncAdapter.class.getSimpleName();

    /**
     * Set up the sync adapter
     */
    public ResponseSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ResponseSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            final ContentProviderClient provider, final SyncResult syncResult) {
        // Check for authtoken
        String token = null;
        try {
            token = am.blockingGetAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, true);
        } catch (OperationCanceledException e) {
            syncResult.stats.numSkippedEntries++;
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
        }

        // If the token wasn't found or there was a problem, we can stop now
        if (token == null || syncResult.stats.numSkippedEntries > 0 ||
            syncResult.stats.numIoExceptions > 0 || syncResult.stats.numAuthExceptions > 0)
            return;

        // Upload responses
        Cursor cursor = null;
        try {
            cursor = provider.query(Responses.CONTENT_URI,
                    new String[]{Responses.SURVEY_ID, Responses.SURVEY_VERSION,
                            Responses.RESPONSE_DATA, Responses.RESPONSE_METADATA,
                            Responses.RESPONSE_EXTRAS}, null, null, null);

            while (cursor.moveToNext()) {
                ohmageService.uploadResponse(cursor.getString(0), cursor.getLong(1),
                        new ResponseTypedOutput(cursor.getString(2), cursor.getString(3),
                                gson.fromJson(cursor.getString(4), ResponseFiles.class)));
            }
            cursor.close();

            // Delete any uploaded responses
            provider.delete(appendSyncAdapterParam(Responses.CONTENT_URI), null, null);

        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
        } catch (AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static Uri appendSyncAdapterParam(Uri uri) {
        return uri.buildUpon().appendQueryParameter(IS_SYNCADAPTER, "true").build();
    }
}
