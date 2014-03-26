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
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.ohmage.provider.OhmageDbHelper.Tables;
import org.ohmage.provider.ResponseContract.Responses;
import org.ohmage.sync.OhmageSyncAdapter;

public class ResponseContentProvider extends ContentProvider {

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int RESPONSES = 0;
    }

    private OhmageDbHelper dbHelper;

    private static UriMatcher sUriMatcher;

    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ResponseContract.CONTENT_AUTHORITY, "responses", MatcherTypes.RESPONSES);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.RESPONSES:
                count = dbHelper.getWritableDatabase().delete(Tables.Responses, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null, !isSyncAdapter(uri));

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.RESPONSES:
                return Responses.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long result = -1;
        String id = null;

        ContentResolver cr = getContext().getContentResolver();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.RESPONSES:
                result = db.insert(Tables.Responses, null, values);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }
        if (result != -1) {
            if (id != null) {
                uri = uri.buildUpon().appendPath(id).build();
            }
            cr.notifyChange(uri, null, !isSyncAdapter(uri));
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new OhmageDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor cursor;
        String id;
        Long version;
        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.RESPONSES:
                cursor = dbHelper.getReadableDatabase().query(Tables.Responses, projection,
                        selection, selectionArgs, null, null, sortOrder);
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

    private boolean isSyncAdapter(Uri uri) {
        return uri.getQueryParameter(OhmageSyncAdapter.IS_SYNCADAPTER) != null;
    }
}
