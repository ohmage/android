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

/**
 * Created by cketcham on 12/11/13.
 */

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;

import com.google.gson.stream.JsonWriter;

import org.ohmage.models.Stream;
import org.ohmage.streams.StreamContract;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import retrofit.mime.TypedOutput;

/**
 * Streams data in batches from a {@link DeletingCursor} of {@link org.ohmage.models.Stream}s until
 * the max number of points or max number of bytes is reached.
 */
public class StreamWriterOutput implements TypedOutput {

    private final ContentProviderClient mProvider;

    private DeletingCursor mCursor;

    private JsonWriter writer;

    private OutputStreamWriter mOut;

    private long mSize;

    private int mNum;

    private long mCurrentSize;

    private int mCurrentNum;

    /**
     * Uploaded in batches based on the size of the points
     */
    public static final long BATCH_MAX_SIZE_BYTES = 1024 * 256;

    /**
     * Uploaded in batches based on the number of points
     */
    public static final int BATCH_MAX_COUNT = 200;

    private interface ProbeQuery {
        static final String[] PROJECTION = new String[]{
                StreamContract.Streams._ID,
                StreamContract.Streams.STREAM_METADATA,
                StreamContract.Streams.STREAM_DATA
        };

        static final int PROBE_METADATA = 1;

        static final int PROBE_DATA = 2;
    }

    private final static String pointData = "\"data\":%s";

    private final static String pointMetaData = "\"meta_data\":%s";

    /**
     * Create a {@link StreamWriterOutput} that reads data from the given provider and uploads
     * data in batches which has a maximum size in bytes, or a maximum number of points per batch.
     * Whichever comes first.
     *
     * @param provider
     * @param size
     * @param num
     */
    public StreamWriterOutput(ContentProviderClient provider, long size, int num) {
        mProvider = provider;
        mSize = size;
        mNum = num;
    }

    /**
     * Create a {@link StreamWriterOutput} that reads data from the given provider and uploads
     * data in batches which has a maximum size of {@link #BATCH_MAX_SIZE_BYTES}, or
     * {@link #BATCH_MAX_COUNT} number of points per batch. Whichever comes first.
     *
     * @param provider
     */
    public StreamWriterOutput(ContentProviderClient provider) {
        this(provider, BATCH_MAX_SIZE_BYTES, BATCH_MAX_COUNT);
    }

    /**
     * Queries for the first batch of points for this account and stream
     *
     * @param accountName
     * @param stream
     * @throws RemoteException
     */
    public void query(String accountName, Stream stream) throws RemoteException {
        setCursor(mProvider.query(
                StreamContract.Streams.CONTENT_URI,
                ProbeQuery.PROJECTION,
                StreamContract.Streams.USERNAME + "=? AND "
                + StreamContract.Streams.STREAM_ID + "=? AND "
                + StreamContract.Streams.STREAM_VERSION + "=?", new String[]{
                accountName, stream.schemaId, String.valueOf(stream.schemaVersion)
        }, null));
    }

    public void setCursor(Cursor cursor) {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        if (cursor == null)
            return;

        if (!(cursor instanceof DeletingCursor))
            cursor = new DeletingCursor(cursor);
        mCursor = (DeletingCursor) cursor;
    }

    public boolean moveToNextBatch() throws RemoteException {
        ensureNoDanglingPoints();
        reset();
        if (mCursor != null) {
            return !mCursor.isAfterLast();
        }

        return false;
    }

    public int deleteBatch() throws RemoteException {
        if (mCursor == null)
            return 0;

        return mCursor.deleteMarked(mProvider, StreamContract.Streams.CONTENT_URI);
    }

    /**
     * Instead of deleting a batch, we can restart it in the case of certain errors
     */
    public void restartBatch() {
        if (mCursor == null)
            return;

        mCursor.restart();
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override public String fileName() {
        return null;
    }

    @Override public String mimeType() {
        return "application/json";
    }

    @Override public long length() {
        return -1;
    }

    @Override public void writeTo(OutputStream out) throws IOException {
        if (mCursor == null)
            throw new RuntimeException("Cursor was never set");

        if (mCurrentSize == 0 || mCurrentNum == 0)
            return;

        mOut = new OutputStreamWriter(out);
        writer = new JsonWriter(mOut);
        writer.beginArray();
        // the moveToNext call must be last, otherwise it will assume it was added to the batch
        while (mCurrentSize > 0 && mCurrentNum > 0 && mCursor.moveToNext()) {
            mCurrentSize -= writePoint();
            mCurrentNum--;
        }
        writer.endArray();
        writer.flush();
    }

    private void reset() {
        mCurrentSize = mSize;
        mCurrentNum = mNum;
    }

    /**
     * Write the contents of a point from the cursor. The {@link JsonWriter} does not have a way
     * to write json objects that are already strings so this method starts an object and then
     * writes directly to the {@link java.io.OutputStream}. It doesn't check the validity of the
     * json objects for efficiency.
     *
     * @return the number of bytes written to the stream
     * @throws java.io.IOException thrown if there is a problem writing to the stream
     */
    private long writePoint() throws IOException {
        writer.beginObject();

        StringBuilder builder = new StringBuilder();

        String data = mCursor.getString(ProbeQuery.PROBE_DATA);
        if (!TextUtils.isEmpty(data)) {
            builder.append(String.format(pointData, data));
        }
        String metadata = mCursor.getString(ProbeQuery.PROBE_METADATA);
        if (!TextUtils.isEmpty(metadata)) {
            if (builder.length() != 0) builder.append(",");
            builder.append(String.format(pointMetaData, metadata));
        }

        mOut.write(builder.toString());
        writer.endObject();
        return Utf8LenCounter.length(builder);
    }

    private void ensureNoDanglingPoints() {
        if (mCursor != null && mCursor.hasPointsToDelete()) {
            throw new RuntimeException(
                    "deleteBatch() must be called since there are some points to delete");
        }
    }

    public static class Utf8LenCounter {
        public static int length(CharSequence sequence) {
            int count = 0;
            for (int i = 0, len = sequence.length(); i < len; i++) {
                char ch = sequence.charAt(i);
                if (ch <= 0x7F) {
                    count++;
                } else if (ch <= 0x7FF) {
                    count += 2;
                } else if (Character.isHighSurrogate(ch)) {
                    count += 4;
                    ++i;
                } else {
                    count += 3;
                }
            }
            return count;
        }
    }
}
