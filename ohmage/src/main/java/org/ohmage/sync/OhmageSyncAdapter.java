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
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.Authenticator;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Stream;
import org.ohmage.models.Survey;
import org.ohmage.models.User;
import org.ohmage.operators.ContentProviderSaver.ContentProviderSaverObserver;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Responses;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.sync.ResponseTypedOutput.ResponseFiles;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.RetrofitError;
import rx.Observable;
import rx.observables.BlockingObservable;
import rx.util.functions.Func1;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class OhmageSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String IS_SYNCADAPTER = "is_syncadapter";

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

        Log.d(TAG, "state of ohmlets sync");
        final String userId = am.getUserData(account, Authenticator.USER_ID);

        // TODO: add modififed flag and timestamp to know which things to upload to the server

        // First, sync ohmlet join state. As described by the people field.
        Cursor cursor = null;
        try {
            cursor = provider.query(OhmageContract.Ohmlets.CONTENT_URI,
                    new String[]{
                            OhmageContract.Ohmlets.OHMLET_ID,
                            OhmageContract.Ohmlets.OHMLET_MEMBERS},
                    null, null, null);

            while (cursor.moveToNext()) {
                Member.List members = gson.fromJson(cursor.getString(1), Member.List.class);
                final Member localMember = members.getMember(userId);

                BlockingObservable.from(ohmageService.getOhmlet(cursor.getString(0))).first(
                        new Func1<Ohmlet, Boolean>() {
                            @Override public Boolean call(Ohmlet ohmlet) {
                                Member remoteMember = ohmlet.people.getMember(userId);
                                try {
                                    if (localMember != null) {
                                        if (remoteMember == null ||
                                            localMember.role != remoteMember.role) {
                                            // Check for join verification code to send
                                            if (localMember.code != null) {
                                                String code = localMember.code;
                                                localMember.code = null;
                                                ohmageService.updateMemberForOhmlet(ohmlet.ohmletId,
                                                        localMember, code);
                                            } else {
                                                ohmageService.updateMemberForOhmlet(ohmlet.ohmletId,
                                                        localMember);
                                            }
                                        }
                                    }
                                    if (localMember == null && remoteMember != null) {
                                        ohmageService.removeUserFromOhmlet(ohmlet.ohmletId, userId);
                                    }

                                    if (localMember == null && remoteMember != null) {
                                    } else if (remoteMember != null) {

                                    }
                                } catch (AuthenticationException e) {
                                    syncResult.stats.numAuthExceptions++;
                                } catch (RetrofitError e) {
                                    syncResult.stats.numIoExceptions++;
                                }
                                return true;
                            }
                        });
            }
            cursor.close();

            // Don't continue if there are errors above
            if (syncResult.stats.numIoExceptions > 0 || syncResult.stats.numAuthExceptions > 0)
                return;

            // Second, synchronize all data
            // TODO: this probably needs to be in a transaction some how? It needs to deal with the case where it wants to sync an ohmlet that the user is trying to interact with at the same time.
            // TODO: handle errors that occur here using the syncResult
            Observable<Ohmlet> ohmlets = ohmageService.getCurrentStateForUser(userId).flatMap(
                    new Func1<User, Observable<Ohmlet>>() {
                        @Override
                        public Observable<Ohmlet> call(User user) {
                            return Observable.from(user.ohmlets);
                        }
                    }).flatMap(new RefreshOhmlet()).cache();
            ohmlets.subscribe(new ContentProviderSaverObserver(true));
            ohmlets.flatMap(new SurveysFromOhmlet()).filter(new FilterUpToDateSurveys(provider))
                    .flatMap(new RefreshSurvey()).subscribe(new ContentProviderSaverObserver(true));
            ohmlets.flatMap(new StreamsFromOhmlet()).filter(new FilterUpToDateStreams(provider))
                    .flatMap(new RefreshStream()).subscribe(new ContentProviderSaverObserver(true));

            // TODO: clean up old ohmlets

            // TODO: download streams and surveys that are not part of ohmlets

        } catch (AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
        } finally {
            if (cursor != null)
                cursor.close();
        }


        // Upload responses
        cursor = null;
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

    public static class SurveysFromOhmlet implements Func1<Ohmlet, Observable<Survey>> {
        @Override public Observable<Survey> call(Ohmlet ohmlet) {
            return Observable.from(ohmlet.surveys);
        }
    }

    public static class StreamsFromOhmlet implements Func1<Ohmlet, Observable<Stream>> {
        @Override public Observable<Stream> call(Ohmlet ohmlet) {
            return Observable.from(ohmlet.streams);
        }
    }

    public class RefreshStream implements Func1<Stream, Observable<Stream>> {
        @Override public Observable<Stream> call(Stream stream) {
            return ohmageService.getStream(stream.schemaId, stream.schemaVersion);
        }
    }

    public class RefreshSurvey implements Func1<Survey, Observable<Survey>> {
        @Override public Observable<Survey> call(Survey survey) {
            return ohmageService.getSurvey(survey.schemaId, survey.schemaVersion);
        }
    }

    public class RefreshOhmlet implements Func1<Ohmlet, Observable<Ohmlet>> {
        @Override public Observable<Ohmlet> call(Ohmlet ohmlet) {
            return ohmageService.getOhmlet(ohmlet.ohmletId);
        }
    }

    public static class FilterUpToDateSurveys implements Func1<Survey, Boolean> {

        private final ContentProviderClient provider;

        public FilterUpToDateSurveys(ContentProviderClient provider) {
            this.provider = provider;
        }

        @Override public Boolean call(Survey survey) {
            try {
                Cursor c = provider.query(
                        Surveys.getUriForSurveyIdVersion(survey.schemaId, survey.schemaVersion),
                        new String[]{Surveys.SURVEY_ID, Surveys.SURVEY_VERSION}, null, null, null);
                if (c.moveToFirst()) {
                    return !c.getString(0).equals(survey.schemaId) ||
                           c.getInt(1) != survey.schemaVersion;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public static class FilterUpToDateStreams implements Func1<Stream, Boolean> {

        private final ContentProviderClient provider;

        public FilterUpToDateStreams(ContentProviderClient provider) {
            this.provider = provider;
        }

        @Override public Boolean call(Stream stream) {
            try {
                Cursor c = provider.query(
                        Streams.getUriForStreamIdVersion(stream.schemaId, stream.schemaVersion),
                        new String[]{Streams.STREAM_ID, Streams.STREAM_VERSION}, null, null, null);
                if (c.moveToFirst()) {
                    return !c.getString(0).equals(stream.schemaId) ||
                           c.getInt(1) != stream.schemaVersion;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public static Uri appendSyncAdapterParam(Uri uri) {
        return uri.buildUpon().appendQueryParameter(IS_SYNCADAPTER, "true").build();
    }
}
