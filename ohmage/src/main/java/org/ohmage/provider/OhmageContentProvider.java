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

package org.ohmage.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import org.ohmage.provider.OhmageDbHelper.Tables;
import org.ohmage.streams.StreamContract;

public class OhmageContentProvider extends ContentProvider {

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int OHMLETS = 0;
        int OHMLETS_ID = 1;
    }

    private OhmageDbHelper dbHelper;

    private static UriMatcher sUriMatcher;

    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(StreamContract.CONTENT_AUTHORITY, "ohmlets", MatcherTypes.OHMLETS);
        sUriMatcher
                .addURI(StreamContract.CONTENT_AUTHORITY, "ohmlets/*/*", MatcherTypes.OHMLETS_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.OHMLETS:
                return OhmageContract.Ohmlets.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;

        ContentResolver cr = getContext().getContentResolver();

        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.OHMLETS:
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.insert(Tables.Ohmlets, BaseColumns._ID, values);

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
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException("query(): Unknown URI: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not allowed");
    }
}
