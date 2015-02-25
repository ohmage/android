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
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.DataPoint;
import org.ohmage.models.SchemaId;
import org.ohmage.models.Survey;
import org.ohmage.provider.ResponseContract;
import org.ohmage.provider.ResponseContract.Responses;
import org.ohmage.sync.ResponseTypedOutput.ResponseFiles;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import retrofit.client.Response;
import rx.Observable;
import rx.Subscriber;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

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
        Observable<Long> toDelete = null;
        Observable<ResponseFiles> filesToDelete = null;

        Cursor cursor = null;

        try {
            cursor = provider.query(Responses.CONTENT_URI,
                    new String[]{BaseColumns._ID, Responses.SURVEY_ID, Responses.SURVEY_VERSION,
                            Responses.RESPONSE_DATA, Responses.RESPONSE_METADATA,
                            Responses.RESPONSE_EXTRAS}, null, null, null);

            while (cursor.moveToNext()) {
                final ResponseFiles files = gson.fromJson(cursor.getString(5), ResponseFiles.class);
                try {
                    // Make the call to upload responses
                    Observable<Response> uploadResponse = null;

                    if(Ohmage.USE_DSU_DATAPOINTS_API) {
                        uploadResponse = uploadDatapoint(cursor);
                    } else {
                        uploadOhmagePoint(cursor, files);
                    }

                    // Map the data for the upload response to the local id in the db
                    final long localResponseId = cursor.getLong(0);
                    Observable<Long> responseId = uploadResponse.map(new Func1<Response, Long>() {
                        @Override public Long call(Response response) {
                            return localResponseId;
                        }
                    });

                    if(toDelete == null) {
                        toDelete = responseId;
                    } else {
                        toDelete = Observable.mergeDelayError(responseId, toDelete);
                    }

                    // Map the data for the upload response to the files in the db
                    Observable<ResponseFiles> responseFiles = uploadResponse.map(new Func1<Response, ResponseFiles>() {
                        @Override public ResponseFiles call(Response response) {
                            return files;
                        }
                    });

                    if(filesToDelete == null) {
                        filesToDelete = responseFiles;
                    } else {
                        filesToDelete = Observable.mergeDelayError(responseFiles, filesToDelete);
                    }
                } catch (AuthenticationException e) {
                    syncResult.stats.numAuthExceptions++;
                }
            }
            cursor.close();



        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if(toDelete != null) {
            toDelete.flatMap(new Func1<Long, Observable<ContentProviderOperation>>() {
                @Override public Observable<ContentProviderOperation> call(Long aLong) {
                    return Observable.from(ContentProviderOperation.newDelete(appendSyncAdapterParam(
                            ContentUris.withAppendedId(Responses.CONTENT_URI, aLong))).build());
                }
            }).subscribe(new Subscriber<ContentProviderOperation>() {
                ArrayList<ContentProviderOperation> toDelete =
                        new ArrayList<ContentProviderOperation>();

                @Override public void onCompleted() {
                    try {
                        getContext().getContentResolver()
                                .applyBatch(ResponseContract.CONTENT_AUTHORITY, toDelete);
                    } catch (RemoteException e) {
                        syncResult.stats.numIoExceptions++;
                    } catch (OperationApplicationException e) {
                        syncResult.stats.numIoExceptions++;
                    }
                    unsubscribe();
                }

                @Override public void onError(Throwable e) {
                    // Delete the successful ones
                    onCompleted();
                }

                @Override public void onNext(ContentProviderOperation args) {
                    toDelete.add(args);
                }
            });
        }

        if(filesToDelete != null) {
            filesToDelete.doOnNext(new Action1<ResponseFiles>() {
                @Override public void call(ResponseFiles responseFiles) {
                    for (String s : responseFiles.getIds()) {
                        responseFiles.getFile(s).delete();
                    }
                }
            }).subscribe(new Subscriber<ResponseFiles>() {
                @Override public void onCompleted() {
                    unsubscribe();
                }

                @Override public void onError(Throwable e) {

                }

                @Override public void onNext(ResponseFiles responseFiles) {

                }
            });
        }
    }

    public static Uri appendSyncAdapterParam(Uri uri) {
        return uri.buildUpon().appendQueryParameter(IS_SYNCADAPTER, "true").build();
    }

    private Observable<Response> uploadOhmagePoint(Cursor cursor, ResponseFiles files) throws AuthenticationException {
        // Make the call to upload responses
        return ohmageService.uploadResponse(cursor.getString(1), cursor.getLong(2),
                new ResponseTypedOutput(cursor.getString(3),
                        cursor.getString(4), files)).cache();

    }

    private Observable<Response> uploadDatapoint(Cursor cursor) throws AuthenticationException {
        DataPoint data = new DataPoint();
        data.schemaId = new SchemaId(cursor.getString(1), cursor.getString(2));

        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> metadata = gson.fromJson(cursor.getString(4), type);
        data.creationDateTime = metadata.get("timestamp");
        data.id = metadata.get("id");

        DataPointTypedOutput point = new DataPointTypedOutput(gson.toJson(data), cursor.getString(3));

        // Make the call to upload responses
        return ohmageService.uploadDataPoint(point).cache();
    }
}
