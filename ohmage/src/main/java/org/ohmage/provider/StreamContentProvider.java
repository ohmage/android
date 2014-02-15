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

package org.ohmage.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.ohmage.app.Ohmage;
import org.ohmage.auth.AuthUtil;
import org.ohmage.provider.OhmageDbHelper.Tables;
import org.ohmage.streams.AsyncBulkInsertHandler;
import org.ohmage.streams.StreamContract;

import javax.inject.Inject;

public class StreamContentProvider extends ContentProvider implements OnAccountsUpdateListener {

    @Inject AccountManager am;

    private AsyncBulkInsertHandler mAsyncQueryHandler;

    private String mAccount;

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int STREAMS = 0;

        int STREAMS_ID = 1;

        int COUNTS = 2;
    }

    private OhmageDbHelper dbHelper;

    private static UriMatcher sUriMatcher;

    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(StreamContract.CONTENT_AUTHORITY, "streams", MatcherTypes.STREAMS);
        sUriMatcher
                .addURI(StreamContract.CONTENT_AUTHORITY, "streams/*/*", MatcherTypes.STREAMS_ID);
        sUriMatcher.addURI(StreamContract.CONTENT_AUTHORITY, "counts", MatcherTypes.COUNTS);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.STREAMS:
                count = dbHelper.getWritableDatabase().delete(Tables.Streams, selection,
                        selectionArgs);
                break;
            case MatcherTypes.COUNTS:

            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }

        notifyInsert(uri, count);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.STREAMS:
                return StreamContract.Streams.CONTENT_TYPE;
            case MatcherTypes.COUNTS:
                return StreamContract.StreamCounts.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
    }

    public static int counter = 0;

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;

        ContentResolver cr = getContext().getContentResolver();

        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.STREAMS:

                if (mAccount == null) {
                    return null;
                }

                // Set the correct username
                values.put(StreamContract.Streams.USERNAME, mAccount);

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.insert(Tables.Streams, BaseColumns._ID, values);

                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }
        if (id != -1)
            return ContentUris.withAppendedId(StreamContract.Streams.CONTENT_URI, id);
        return null;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new OhmageDbHelper(getContext());

        ((Ohmage) getContext().getApplicationContext()).getApplicationGraph().inject(this);

        am.addOnAccountsUpdatedListener(this, null, true);

        mAsyncQueryHandler = new AsyncBulkInsertHandler(getContext().getContentResolver(),
                StreamContract.Streams.CONTENT_URI);
        return true;
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        for (Account account : accounts) {
            if (AuthUtil.ACCOUNT_TYPE.equals(account.type)) {
                mAccount = account.name;
                return;
            }
        }
        mAccount = null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.COUNTS:
                SQLiteQueryBuilder streams = new SQLiteQueryBuilder();
                streams.setTables(Tables.Streams);
                cursor = streams.query(dbHelper.getReadableDatabase(), projection, selection,
                        selectionArgs, StreamContract.Streams.STREAM_ID + ", "
                                       + StreamContract.Streams.STREAM_VERSION, null, sortOrder);
                break;
            case MatcherTypes.STREAMS:
                cursor = dbHelper.getReadableDatabase()
                                 .query(Tables.Streams, projection, selection,
                                         selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("query(): Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not allowed");
    }

    @Override
    public synchronized int bulkInsert(Uri uri, ContentValues[] values) {
        int count = 0;

        // We can't insert any points if no account exists.
        String account = mAccount;
        if (TextUtils.isEmpty(account)) {
            return 0;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            String table;
            switch (sUriMatcher.match(uri)) {
                case MatcherTypes.STREAMS:
                    table = Tables.Streams;
                    break;
                default:
                    throw new UnsupportedOperationException("bulkInsert(): Unknown URI: " + uri);
            }

            for (ContentValues v : values) {
                v.put(StreamContract.Streams.USERNAME, mAccount);
                if (db.insert(table, BaseColumns._ID, v) != -1)
                    count++;
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyInsert(uri, count);

        return count;
    }

    private void notifyInsert(Uri uri, Integer count) {
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();

            switch (sUriMatcher.match(uri)) {
                case MatcherTypes.STREAMS:
                    cr.notifyChange(StreamContract.Streams.CONTENT_URI, null, false);
                    break;
            }
        }
    }
}
