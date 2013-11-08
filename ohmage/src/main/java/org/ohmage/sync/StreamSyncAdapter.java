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
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import org.ohmage.app.Endpoints;
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.Stream;
import org.ohmage.models.Streams;
import org.ohmage.streams.StreamContract;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class StreamSyncAdapter extends AbstractThreadedSyncAdapter {

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

    /**
     * Uploaded in batches based on the size of the points
     */
    private static final long BATCH_MAX_SIZE_MB = 1024 * 256;

    /**
     * Uploaded in batches based on the number of points
     */
    private static final int BATCH_MAX_COUNT = 200;

    private static final String SQL_AND = " AND %s='%s'";

    private AccountManager mAccountManager;

    private OkHttpClient mClient;

    /**
     * Set to true if we had an auth error once. If we have another auth error, we will stop the
     * sync.
     */
    private boolean mHadAuthError;

    /**
     * The id of stream we are uploading in this sync
     */
    private String mStreamId;

    /**
     * The version of the stream we are uploading in this sync
     */
    private String mStreamVersion;

    /**
     * Set up the sync adapter
     */
    public StreamSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
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
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d("ohmage", "start stream sync");

        mClient = new OkHttpClient();
        mAccountManager = AccountManager.get(getContext());

        mStreamId = extras.getString(EXTRA_STREAM_ID);
        if (!TextUtils.isEmpty(mStreamId))
            mStreamVersion = extras.getString(EXTRA_STREAM_VERSION);

        // Get a list of all the streams that are in the db
        Streams streams = getStreams(account, provider);
        for (Stream stream : streams) {

            DeletingCursor c = null;
            try {
                c = new DeletingCursor(provider.query(
                        StreamContract.Streams.CONTENT_URI,
                        ProbeQuery.PROJECTION,
                        StreamContract.Streams.USERNAME + "=? AND "
                                + StreamContract.Streams.STREAM_ID + "=? AND "
                                + StreamContract.Streams.STREAM_VERSION + "=?", new String[]{
                        account.name, stream.id, stream.version
                }, null));

                while (c.moveToNext()) {
                    long startTime = System.currentTimeMillis();
                    sendData(account, stream, c);
                    int count = c.deleteMarked(provider, StreamContract.Streams.CONTENT_URI);
                    Log.d(TAG, "uploaded batch of " + count + " points in: "
                            + (System.currentTimeMillis() - startTime));

                }
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                syncResult.stats.numIoExceptions++;
            } catch (AuthFailureError e) {
                syncResult.stats.numAuthExceptions++;
            } finally {
                if (c != null)
                    c.close();
            }
        }

//        /**
//         * Used to indicate that the SyncAdapter experienced a hard error due to trying the same
//         * operation too many times (as defined by the SyncAdapter). The SyncManager will record
//         * that the sync request failed and it will not reschedule the request.
//         */
//        public boolean tooManyRetries;
//
//        /**
//         * Used to indicate that the SyncAdapter experienced a hard error due to an error it
//         * received from interacting with the storage later. The SyncManager will record that
//         * the sync request failed and it will not reschedule the request.
//         */
//        public boolean databaseError;
//
//
//        /**
//         * The SyncAdapter was unable to authenticate the {@link android.accounts.Account}
//         * that was specified in the request. The user needs to take some action to resolve
//         * before a future request can expect to succeed. This is considered a hard error.
//         */
//        public long numAuthExceptions;
//
//
//        /**
//         * The SyncAdapter had a problem, most likely with the network connectivity or a timeout
//         * while waiting for a network response. The request may succeed if it is tried again
//         * later. This is considered a soft error.
//         */
//        public long numIoExceptions;
//
//        /**
//         * The SyncAdapter had a problem with the data it received from the server or the storage
//         * later. This problem will likely repeat if the request is tried again. The problem
//         * will need to be cleared up by either the server or the storage layer (likely with help
//         * from the user). If the SyncAdapter cleans up the data itself then it typically won't
//         * increment this value although it may still do so in order to record that it had to
//         * perform some cleanup. E.g., if the SyncAdapter received a bad entry from the server
//         * when processing a feed of entries, it may choose to drop the entry and thus make
//         * progress and still increment this value just so the SyncAdapter can record that an
//         * error occurred. This is considered a hard error.
//         */
//        public long numParseExceptions;
//
//        /**
//         * The SyncAdapter detected that there was an unrecoverable version conflict when it
//         * attempted to update or delete a version of a resource on the server. This is expected
//         * to clear itself automatically once the new state is retrieved from the server,
//         * though it may remain until the user intervenes manually, perhaps by clearing the
//         * local storage and starting over frmo scratch. This is considered a hard error.
//         */
//        public long numConflictDetectedExceptions;
//
//        /**
//         * Counter for tracking how many deletes were performed by the sync operation, as defined
//         * by the SyncAdapter.
//         */
//        public long numDeletes;

    }

    /**
     * Queries the db to get a list of stream version pairs
     *
     * @param account
     * @param provider
     * @return
     */
    private Streams getStreams(Account account, ContentProviderClient provider) {
        Streams streams = new Streams();

        StringBuilder select = new StringBuilder(StreamContract.Streams.USERNAME + "=?");

        if (mStreamId != null) {
            select.append(String.format(SQL_AND, StreamContract.Streams.STREAM_ID, mStreamId));
        }

        if (mStreamVersion != null) {
            select.append(String.format(SQL_AND,
                    StreamContract.Streams.STREAM_VERSION, mStreamVersion));
        }

        Cursor cursor = null;
        try {
            cursor = provider.query(StreamContract.Streams.CONTENT_URI,
                    new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                            StreamContract.Streams.STREAM_VERSION},
                    select.toString(), new String[]{account.name}, null);

            while (cursor.moveToNext()) {
                Stream s = new Stream();
                s.id = cursor.getString(0);
                s.version = cursor.getString(1);
                streams.add(s);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return streams;
    }

    /**
     * Streams points over the network to the ohmage server.
     *
     * @param account the account we are syncing
     * @param stream  the stream we are currently syncing
     * @param data    A reference to the data via a {@link DeletingCursor}
     * @return true if the data was successfully sent, false if there was an error with some points
     * in this batch
     * @throws AuthenticatorException     thrown if there is a problem talking to the ohmage
     *                                    authenticator
     * @throws IOException                thrown if there is an error communicating with the server
     * @throws AuthFailureError           thrown if there was a problem authenticating with the
     *                                    server (ie. the refresh token didn't work and we need user
     *                                    intervention.
     * @throws OperationCanceledException thrown if the operation to get an authtoken was cancelled
     *                                    for whatever reason
     */
    private boolean sendData(Account account, Stream stream, DeletingCursor data) throws
            AuthenticatorException, IOException, AuthFailureError, OperationCanceledException {
        String token = mAccountManager.blockingGetAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, true);
        Log.d(TAG, "token=" + token);
        if (token == null)
            throw new AuthFailureError();

        URL url = new URL(Endpoints.Stream.data(stream) + "?auth_token=" + token);
        HttpURLConnection connection = mClient.open(url);
        OutputStream out = null;
        InputStream in = null;

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            out = new BufferedOutputStream(connection.getOutputStream());
            ProbeWriterBody probeWriter = new ProbeWriterBody(out, data, BATCH_MAX_SIZE_MB, BATCH_MAX_COUNT);
            probeWriter.writeJson();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {

                in = new BufferedInputStream(connection.getErrorStream());
                BufferedReader isr = new BufferedReader(new InputStreamReader(in));

                Log.d(TAG, "failed to upload data");
                Log.d(TAG, connection.getResponseCode() + ": " + connection.getResponseMessage());
                Log.d(TAG, "message= " + isr.readLine());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // If the response failed because of an auth error, we will try one more time
                    if (!mHadAuthError) {
                        mHadAuthError = true;
                        mAccountManager.invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, token);
                        return sendData(account, stream, data);
                    } else {
                        throw new AuthFailureError();
                    }
                } else {
                    mHadAuthError = false;
                }

                //TODO: figure out which errors should stop the sync completely and which should continue
                return false;
            }
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
            if (connection != null) connection.disconnect();
        }

        return true;
    }

    private interface ProbeQuery {
        static final String[] PROJECTION = new String[]{
                StreamContract.Streams._ID,
                StreamContract.Streams.STREAM_METADATA,
                StreamContract.Streams.STREAM_DATA
        };

        static final int PROBE_METADATA = 1;

        static final int PROBE_DATA = 2;
    }

    /**
     * Streams data from a {@link DeletingCursor} until the max number of points or max number of
     * bytes is reached.
     */
    public static class ProbeWriterBody {

        private final DeletingCursor mCursor;

        private final JsonWriter writer;

        private final OutputStreamWriter mOut;

        private long mSize;

        private int mNum;

        public ProbeWriterBody(OutputStream out, DeletingCursor c, long size, int num) {
            mCursor = c;
            mSize = size;
            mNum = num;

            mOut = new OutputStreamWriter(out);
            writer = new JsonWriter(mOut);
        }

        public void writeJson() throws IOException {
            writer.beginArray();
            // the moveToNext call must be last, otherwise it will assume it was added to the batch
            while (mSize > 0 && mNum > 0 && mCursor.moveToNext()) {
                mSize -= createProbe(mCursor, writer);
                mNum--;
            }
            writer.endArray();
            writer.flush();
        }

        private final static String pointData = "\"data\":%s";

        private final static String pointMetaData = "\"meta_data\":%s";

        /**
         * Write the contents of a probe from the cursor. The {@link JsonWriter} does not have a way
         * to write json objects that are already strings so this method starts an object and then
         * writes directly to the {@link java.io.OutputStream}. It doesn't check the validity of the
         * json objects for efficiency.
         *
         * @param cursor the cursor to read data from
         * @param writer the {@link JsonWriter} to write to
         * @return the number of bytes written to the stream
         * @throws IOException thrown if there is a problem writing to the stream
         */
        private long createProbe(Cursor cursor, JsonWriter writer) throws IOException {
            writer.beginObject();

            StringBuilder builder = new StringBuilder();

            String data = cursor.getString(ProbeQuery.PROBE_DATA);
            if (!TextUtils.isEmpty(data)) {
                builder.append(String.format(pointData, data));
            }
            String metadata = cursor.getString(ProbeQuery.PROBE_METADATA);
            if (!TextUtils.isEmpty(metadata)) {
                if (builder.length() != 0) builder.append(",");
                builder.append(String.format(pointMetaData, metadata));
            }

            mOut.write(builder.toString());
            writer.endObject();
            return builder.length();
        }
    }


    /**
     * A cursor that handles deletions while the data is still being read. The normal cursor will
     * not update its position correctly
     */
    public static class DeletingCursor extends CursorWrapper {

        private final LinkedList<Long> ids = new LinkedList<Long>();

        private final int mCount;

        private int mOffset = 0;

        /**
         * The number of deleted points
         */
        private int mDeleted;

        public DeletingCursor(Cursor c) {
            super(c);
            mCount = super.getCount();
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int getPosition() {
            return super.getPosition() + mOffset;
        }

        @Override
        public boolean move(int offset) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToPosition(int position) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToFirst() {
            if (!isBeforeFirst())
                throw new RuntimeException("DeletingCursor can only move forwards");
            return super.moveToFirst();
        }

        @Override
        public boolean moveToLast() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToNext() {
            if (!isAfterLast() && !isLast() && super.getPosition() + 1 > getActualCount()) {
                requery();
            }
            if (super.moveToNext()) {
                ids.add(getLong(0));
                return true;
            }
            return false;
        }

        @Override
        public boolean moveToPrevious() {
            throw new RuntimeException("DeletingCursor can only move forwards");
        }

        @Override
        public boolean isFirst() {
            return super.isFirst() && !deletedPoints();
        }

        @Override
        public boolean isBeforeFirst() {
            return super.isBeforeFirst() && !deletedPoints();
        }

        /**
         * Calculates the actual number of points that have not been deleted yet
         *
         * @return the number of points
         */
        public int getActualCount() {
            return super.getCount() - mDeleted;
        }

        /**
         * Check if this cursor has deleted points
         *
         * @return true if it has deleted points
         */
        public boolean deletedPoints() {
            return !ids.isEmpty() || mDeleted != 0;
        }

        /**
         * Delete all points that have been marked so far
         *
         * @param provider
         * @param contentUri
         * @return
         * @throws RemoteException
         */
        public int deleteMarked(ContentProviderClient provider, Uri contentUri)
                throws RemoteException {
            StringBuilder deleteString = new StringBuilder();

            // Deleting this batch of points. We can only delete
            // with a maximum expression tree depth of 1000
            int batch = 0;
            for (Long id : ids) {
                if (deleteString.length() != 0)
                    deleteString.append(" OR ");
                deleteString.append(BaseColumns._ID + "=" + id);
                batch++;

                // If we have 1000 Expressions or we are at the last
                // point, delete them
                if ((batch % (1000 - 2) == 0) || batch == ids.size()) {
                    provider.delete(contentUri, deleteString.toString(), null);
                    deleteString = new StringBuilder();
                }
            }
            int count = ids.size();
            mDeleted += count;
            ids.clear();
            return count;
        }

        @Override
        @Deprecated
        public boolean requery() {
            Log.d(TAG, "requery");
            int position = super.getPosition();
            if (super.requery()) {
                super.move(position - mDeleted + 1);
                mOffset += position - (position - mDeleted);
                mDeleted = 0;
                return true;
            }
            return false;
        }
    }
}
