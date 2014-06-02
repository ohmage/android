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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.InstallSurveyDependencies;
import org.ohmage.app.MainActivity;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.Authenticator;
import org.ohmage.models.ApkSet;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Stream;
import org.ohmage.models.Survey;
import org.ohmage.models.User;
import org.ohmage.operators.ContentProviderSaver.ContentProviderSaverSubscriber;
import org.ohmage.operators.ContentProviderStateSync.ContentProviderStateSyncSubscriber;
import org.ohmage.prompts.Prompt;
import org.ohmage.prompts.RemotePrompt;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Ohmlets;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observers.SafeSubscriber;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class OhmageSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String IS_SYNCADAPTER = "is_syncadapter";

    private static final int NOTIFICATION_STREAM_APPS_ID = 0;
    private static final int NOTIFICATION_REMOTE_APPS_ID = 1;

    @Inject AccountManager am;

    @Inject OhmageService ohmageService;

    @Inject Gson gson;

    private static final String TAG = OhmageSyncAdapter.class.getSimpleName();
    private SyncResult mSyncResult;

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

        mSyncResult = syncResult;

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
            syncResult.stats.numIoExceptions > 0 || syncResult.stats.numAuthExceptions > 0) {
            return;
        }

        String userId = am.getUserData(account, Authenticator.USER_ID);

        try {
            synchronizeOhmlets(userId, provider);
            // TODO: download streams and surveys that are not part of ohmlets

            // Don't continue if there were already errors
            if (mSyncResult.stats.numIoExceptions > 0 || mSyncResult.stats.numAuthExceptions > 0) {
                return;
            }
            synchronizeData(userId, provider);
        } catch (AuthenticationException e) {
            Log.e(TAG, "Error authenticating user", e);
            syncResult.stats.numAuthExceptions++;
        } catch (RemoteException e) {
            Log.e(TAG, "Error synchronizing account", e);
            syncResult.stats.numIoExceptions++;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error synchronizing account", e);
            syncResult.stats.numIoExceptions++;
        }
    }

    private void synchronizeOhmlets(final String userId, ContentProviderClient provider)
            throws AuthenticationException, RemoteException, InterruptedException {
        Log.d(TAG, "state of ohmlets sync");

        final CountDownLatch upload;

        // First, sync ohmlet join state. As described by the people field.
        Cursor cursor = null;
        try {
            cursor = provider.query(OhmageContract.Ohmlets.CONTENT_URI,
                    new String[]{
                            OhmageContract.Ohmlets.OHMLET_ID,
                            OhmageContract.Ohmlets.OHMLET_MEMBERS},
                    null, null, null
            );

            upload = new CountDownLatch(cursor.getCount());

            while (cursor.moveToNext()) {
                Member.List members = gson.fromJson(cursor.getString(1), Member.List.class);
                Member m = members.getMember(userId);
                if (m == null) {
                    m = members.getMember("me");
                    if (m != null) {
                        m.memberId = userId;
                    }
                }
                final Member localMember = m;
                ohmageService.getOhmlet(cursor.getString(0)).first(
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
                                } catch (AuthenticationException e) {
                                    Log.e(TAG, "Error authenticating user", e);
                                    mSyncResult.stats.numAuthExceptions++;
                                } catch (RetrofitError e) {
                                    Log.e(TAG, "Error synchronizing ohmlet member state", e);
                                    mSyncResult.stats.numIoExceptions++;
                                }
                                return true;
                            }
                        }
                ).finallyDo(new Action0() {
                    @Override public void call() {
                        upload.countDown();
                    }
                }).subscribe();
            }
            cursor.close();

            // Wait for the upload sync operation to finish before downloading the user state
            upload.await();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void synchronizeData(String userId, ContentProviderClient provider)
            throws AuthenticationException, RemoteException, InterruptedException {

        final CountDownLatch download = new CountDownLatch(1);

        Observable<Ohmlet> current = ohmageService.getCurrentStateForUser(userId).flatMap(
                new Func1<User, Observable<Ohmlet>>() {
                    @Override
                    public Observable<Ohmlet> call(User user) {
                        return Observable.from(user.ohmlets);
                    }
                }
        ).cache();

        // Synchronize the current ohmlets to the db (this removes other ohmlets)
        current.toList().subscribe(new SyncSubscriber<List<Ohmlet>>(mSyncResult,
                new ContentProviderStateSyncSubscriber(Ohmlets.CONTENT_URI, true)));

        Observable<Ohmlet> refreshedOhmlets = current.flatMap(new RefreshOhmlet());
        refreshedOhmlets.subscribe(
                new SyncSubscriber<Ohmlet>(mSyncResult, new ContentProviderSaverSubscriber(true)));


        Observable<Survey> surveys = current.flatMap(new SurveysFromOhmlet());
        surveys.toList().subscribe(new SyncSubscriber<List<Survey>>(mSyncResult,
                new ContentProviderStateSyncSubscriber(Surveys.CONTENT_URI, true)));


        Observable<Survey> refreshedSurveys =
                surveys.filter(new FilterUpToDateSurveys(provider))
                        .flatMap(new RefreshSurvey());
        refreshedSurveys.subscribe(
                new SyncSubscriber<Survey>(mSyncResult, new ContentProviderSaverSubscriber(true)));
        refreshedSurveys.subscribe(new SyncSubscriber<Survey>(mSyncResult,
                new Subscriber<Survey>() {
                    public ApkSet surveys = new ApkSet();

                    @Override public void onCompleted() {
                        showInstallSurveyApkNotification(surveys);
                    }

                    @Override public void onError(Throwable e) {
                        showInstallSurveyApkNotification(surveys);
                    }

                    @Override public void onNext(Survey survey) {
                        for (Prompt prompt : survey.surveyItems) {
                            if (prompt instanceof RemotePrompt &&
                                ((RemotePrompt) prompt).getApp() != null &&
                                !((RemotePrompt) prompt).getApp()
                                        .isInstalled(getContext())) {
                                surveys.add(((RemotePrompt) prompt).getApp());
                                return;
                            }
                        }
                    }
                }
        ));

        Observable<Stream> streams = current.flatMap(new StreamsFromOhmlet());
        streams.toList().subscribe(new SyncSubscriber<List<Stream>>(mSyncResult,
                new ContentProviderStateSyncSubscriber(Streams.CONTENT_URI, true)));


        Observable<Stream> refreshedStreams =
                streams.filter(new FilterUpToDateStreams(provider)).flatMap(
                        new RefreshStream());
        refreshedStreams.subscribe(
                new SyncSubscriber<Stream>(mSyncResult, new ContentProviderSaverSubscriber(true)));
        refreshedStreams.subscribe(
                new SyncSubscriber<Stream>(mSyncResult, new Subscriber<Stream>() {
                    public ApkSet streams = new ApkSet();

                    @Override public void onCompleted() {
                        showInstallStreamApkNotification(streams);
                    }

                    @Override public void onError(Throwable e) {
                        showInstallStreamApkNotification(streams);
                    }

                    @Override public void onNext(Stream stream) {
                        if (!stream.app.isInstalled(getContext())) {
                            stream.app.android.appName = stream.name;
                            streams.add(stream.app);
                        }
                    }
                })
        );

        Observable.merge(current, refreshedStreams, refreshedSurveys, refreshedOhmlets).last()
                .finallyDo(new Action0() {
                    @Override public void call() {
                        download.countDown();
                    }
                }).subscribe();

        // Wait for any async download operations to finish
        download.await();
    }

    public void showInstallStreamApkNotification(ApkSet streams) {
        if (streams == null || streams.isEmpty()) {
            return;
        }

        Intent resultIntent = new Intent(getContext(), MainActivity.class);
        resultIntent.putExtra(MainActivity.EXTRA_VIEW_STREAMS, true);
        Builder builder = new Builder(getContext());

        if (streams.size() > 1 && VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            builder.setSmallIcon(R.drawable.stat_notify_update_collapse).setContentText(getContext()
                    .getString(R.string.install_multiple_apps_for_stream_message, streams.size()));
        } else {
            builder.setSmallIcon(R.drawable.stat_notify_update).setContentText(getContext()
                    .getString(R.string.install_app_for_stream_message,
                            streams.iterator().next().getAppName()));
        }

        builder.setContentTitle(getContext().getString(R.string.install_apps_for_stream_title));
        showInstallApkNotification(NOTIFICATION_STREAM_APPS_ID, builder, resultIntent);
    }

    public void showInstallSurveyApkNotification(ApkSet surveys) {
        if (surveys == null || surveys.isEmpty()) {
            return;
        }

        Intent resultIntent = new Intent(getContext(), InstallSurveyDependencies.class);
        Builder builder = new Builder(getContext());

        if (surveys.size() > 1 && VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            builder.setSmallIcon(R.drawable.stat_notify_update_collapse).setContentText(getContext()
                    .getString(R.string.install_multiple_apps_for_surveys_message, surveys.size()));
        } else {
            builder.setSmallIcon(R.drawable.stat_notify_update).setContentText(getContext()
                    .getString(R.string.install_apps_for_survey_message,
                            surveys.iterator().next().getAppName()));
        }

        builder.setContentTitle(getContext().getString(R.string.install_apps_for_survey_title));
        showInstallApkNotification(NOTIFICATION_REMOTE_APPS_ID, builder, resultIntent);
    }

    private void showInstallApkNotification(int id, Builder builder, Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager mNotificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
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
            Cursor c = null;
            try {
                c = provider.query(
                        Surveys.getUriForSurveyIdVersion(survey.schemaId, survey.schemaVersion),
                        new String[]{Surveys.SURVEY_ID, Surveys.SURVEY_VERSION}, null, null, null);
                if (c.moveToFirst()) {
                    return !c.getString(0).equals(survey.schemaId) ||
                           c.getInt(1) != survey.schemaVersion;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                if(c != null) {
                    c.close();
                }
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
            Cursor c = null;
            try {
                c = provider.query(
                        Streams.getUriForStreamIdVersion(stream.schemaId, stream.schemaVersion),
                        new String[]{Streams.STREAM_ID, Streams.STREAM_VERSION}, null, null, null);
                if (c.moveToFirst()) {
                    return !c.getString(0).equals(stream.schemaId) ||
                           c.getInt(1) != stream.schemaVersion;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                if(c != null) {
                    c.close();
                }
            }
            return true;
        }
    }

    /**
     * Same as the safe subscriber except it will log errors and continue running
     *
     * @param <T>
     */
    public static class SyncSubscriber<T> extends SafeSubscriber<T> {

        private final SyncResult mSyncResult;

        public SyncSubscriber(SyncResult syncResult, Subscriber<? super T> actual) {
            super(actual);
            mSyncResult = syncResult;
        }

        @Override
        public void onError(Throwable e) {
            synchronized (mSyncResult) {
                mSyncResult.stats.numIoExceptions++;
            }
        }
    }


    public static Uri appendSyncAdapterParam(Uri uri) {
        return uri.buildUpon().appendQueryParameter(IS_SYNCADAPTER, "true").build();
    }
}
