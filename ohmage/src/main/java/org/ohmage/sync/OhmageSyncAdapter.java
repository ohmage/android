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
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.Authenticator;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlets;
import org.ohmage.provider.OhmageContract;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.RetrofitError;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class OhmageSyncAdapter extends AbstractThreadedSyncAdapter {

    @Inject AccountManager am;

    @Inject OhmageService ohmageService;

    @Inject Gson gson;

    private static final String TAG = OhmageSyncAdapter.class.getSimpleName();

    /**
     * Set up the sync adapter
     */
    public OhmageSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public OhmageSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
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

        Log.d(TAG, "state of ohmlets sync");

        try {
            Ohmlets ohmlets =
                    ohmageService.getCurrentStateForUser(am.getUserData(account, Authenticator.USER_ID));

            for (Ohmlet ohmlet : ohmlets) {
                Log.d(TAG, ohmlet.name);
            }

            Cursor local = provider.query(OhmageContract.Ohmlets.CONTENT_URI,
                    new String[]{
                            OhmageContract.Ohmlets.OHMLET_ID,
                            OhmageContract.Ohmlets.OHMLET_VERSION,
                            OhmageContract.Ohmlets.OHMLET_JOIN_STATE},
                    null, null, null);

//            while (local.moveToNext()) {
//                Ohmlet remote = ohmlets.getById(local.getString(0));
//                OhmageContract.Ohmlets.JoinState localState = OhmageContract.Ohmlets.JoinState.values()[local.getInt(2)];
//                if(remote.state != localState) {
//                    if(localState == OhmageContract.Ohmlets.JoinState.REQUESTED) {
//
//                    }
//                }
//            }

            //TODO: reconcile state of ohmlets
                                } catch (RetrofitError e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
        }


//        Survey survey = ohmageService.getSurvey("14d45116-2fe5-4188-8944-9267b4fefacc", 2013121800);
//        ContentValues values = new ContentValues();
//        values.put(OhmageContract.Surveys.SURVEY_ID, survey.schemaId);
//        values.put(OhmageContract.Surveys.SURVEY_VERSION, survey.schemaVersion);
//        values.put(OhmageContract.Surveys.SURVEY_ID, gson.toJson(survey.surveyItems));
//        try {
//            provider.insert(OhmageContract.Surveys.CONTENT_URI, values);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        Log.d(TAG, "survey=" + survey);
        }
    }
