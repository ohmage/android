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

package org.ohmage.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import org.ohmage.streams.AsyncBulkInsertHandler;
import org.ohmage.streams.IStreamReceiver;
import org.ohmage.streams.StreamContract.Streams;

/**
 * This service handles stream data from other apks. It validates information about data sent from
 * different apks and saves it to the db to be uploaded.
 * <p/>
 * This services watches for changes in the account manager and saves stream points according
 * to the unique name for the account that is logged in.
 */
public class StreamReceiver extends Service {

    /**
     * Action to view analytics data for a stream
     */
    public static final String ACTION_VIEW_ANALYTICS = "org.ohmage.stream.ACTION_VIEW_ANALYTICS";

    /**
     * Action to view configuration data for a stream
     */
    public static final String ACTION_CONFIGURE = "org.ohmage.stream.ACTION_CONFIGURE";

    /**
     * Meta data for stream supplied in manifest
     */
    public static final String STREAM_META_DATA = "org.ohmage.StreamReceiver";

    private AsyncBulkInsertHandler mAsyncQueryHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return new IStreamReceiver.Stub() {

            @Override
            public void sendStream(String streamId, int streamVersion, String metadata, String data)
                    throws RemoteException {

                ContentValues values = new ContentValues();
                values.put(Streams.STREAM_ID, streamId);
                values.put(Streams.STREAM_VERSION, streamVersion);
                values.put(Streams.STREAM_METADATA, metadata);
                values.put(Streams.STREAM_DATA, data);
                mAsyncQueryHandler.startInsert(values);
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAsyncQueryHandler = new AsyncBulkInsertHandler(getContentResolver(), Streams.CONTENT_URI);
    }
}
