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

import org.ohmage.provider.OhmageContract.Ohmlets;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.provider.OhmageDbHelper.Tables;
import org.ohmage.sync.OhmageSyncAdapter;

public class OhmageContentProvider extends ContentProvider {

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int OHMLETS = 0;
        int OHMLET_ID = 1;
        int SURVEYS = 2;
        int SURVEY_ID = 3;
        int STREAMS = 4;
        int STREAM_ID = 5;
    }

    private OhmageDbHelper dbHelper;

    private static UriMatcher sUriMatcher;

    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "ohmlets", MatcherTypes.OHMLETS);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "ohmlets/*", MatcherTypes.OHMLET_ID);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "surveys", MatcherTypes.SURVEYS);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "surveys/*", MatcherTypes.SURVEY_ID);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "surveys/*/*", MatcherTypes.SURVEY_ID);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "streams", MatcherTypes.STREAMS);
        sUriMatcher.addURI(OhmageContract.CONTENT_AUTHORITY, "streams/*/*", MatcherTypes.STREAM_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.OHMLET_ID:
                return OhmageContract.Ohmlets.CONTENT_ITEM_TYPE;
            case MatcherTypes.SURVEY_ID:
                return OhmageContract.Surveys.CONTENT_ITEM_TYPE;
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
            case MatcherTypes.OHMLETS:
                if (values.containsKey(Ohmlets.OHMLET_ID)) {
                    id = values.getAsString(Ohmlets.OHMLET_ID);
                }
                result = db.replace(Tables.Ohmlets, null, values);
                break;
            case MatcherTypes.SURVEYS:
                result = db.replace(Tables.Surveys, null, values);
                break;
            case MatcherTypes.STREAMS:
                result = db.replace(Tables.Streams, null, values);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }
        if (result != -1 && id != null) {
            Uri notifyUri = uri.buildUpon().appendPath(id).build();
            cr.notifyChange(notifyUri, null, !isSyncAdapter(uri));
            return notifyUri;
        }
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
        Cursor cursor;
        String id;
        Long version;
        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.OHMLETS:
                cursor = dbHelper.getReadableDatabase().query(Tables.Ohmlets, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case MatcherTypes.OHMLET_ID:
                cursor = dbHelper.getReadableDatabase().query(Tables.Ohmlets, projection,
                        Ohmlets.OHMLET_ID + "=?", new String[]{uri.getLastPathSegment()}, null,
                        null, sortOrder);
                break;
            case MatcherTypes.SURVEYS:
                cursor = dbHelper.getReadableDatabase().query(Tables.Surveys, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case MatcherTypes.SURVEY_ID:
                id = Surveys.getId(uri);
                if(id != null) {
                    selection = Surveys.SURVEY_ID + "=?";
                    selectionArgs = new String[] { id };
                    version = Surveys.getVersion(uri);
                    if(version != null) {
                        selection += " AND " + Surveys.SURVEY_VERSION + "=" + version;
                    }
                }
                cursor = dbHelper.getReadableDatabase().query(Tables.Surveys, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case MatcherTypes.STREAMS:
                cursor = dbHelper.getReadableDatabase().query(Tables.Streams, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case MatcherTypes.STREAM_ID:
                cursor = dbHelper.getReadableDatabase().query(Tables.Streams, projection,
                        Streams.STREAM_ID + "=? AND " + Streams.STREAM_VERSION + "=" +
                        Streams.getVersion(uri), new String[]{Streams.getId(uri)}, null, null,
                        sortOrder);
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
