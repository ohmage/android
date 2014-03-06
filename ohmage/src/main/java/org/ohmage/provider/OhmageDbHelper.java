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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.ohmage.models.Ohmlet.PrivacyState;
import org.ohmage.provider.OhmageContract.Ohmlets;
import org.ohmage.provider.OhmageContract.Responses;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.streams.StreamContract;

public class OhmageDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ohmage.db";

    private static final int DB_VERSION = 7;

    public static final String SQL_AND = " AND %s='%s'";

    public interface Tables {
        static final String Ohmlets = "ohmlets";
        static final String Streams = "streams";
        static final String Surveys = "surveys";
        static final String Responses = "responses";
        static final String StreamData = "stream_data";
    }

    public OhmageDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Ohmlets + " ("
                   + Ohmlets.OHMLET_ID + " TEXT PRIMARY KEY, "
                   + Ohmlets.OHMLET_NAME + " TEXT NOT NULL, "
                   + Ohmlets.OHMLET_DESCRIPTION + " TEXT, "
                   + Ohmlets.OHMLET_SURVEYS + " TEXT, "
                   + Ohmlets.OHMLET_STREAMS + " TEXT, "
                   + Ohmlets.OHMLET_MEMBERS + " TEXT, "
                   + Ohmlets.OHMLET_PRIVACY_STATE + " INTEGER DEFAULT " +
                   PrivacyState.UNKNOWN.ordinal() + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Streams + " ("
                   + Streams.STREAM_ID + " TEXT NOT NULL, "
                   + Streams.STREAM_VERSION + " INTEGER NOT NULL, "
                   + "PRIMARY KEY (" + Streams.STREAM_ID + ", " + Streams.STREAM_VERSION + "));");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Surveys + " ("
                   + Surveys.SURVEY_ID + " TEXT NOT NULL, "
                   + Surveys.SURVEY_VERSION + " INTEGER NOT NULL, "
                   + Surveys.SURVEY_ITEMS + " TEXT NOT NULL,"
                   + Surveys.SURVEY_NAME + " TEXT NOT NULL,"
                   + "PRIMARY KEY (" + Surveys.SURVEY_ID + ", " + Surveys.SURVEY_VERSION + "));");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Responses + " ("
                   + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + Responses.SURVEY_ID + " TEXT NOT NULL, "
                   + Responses.SURVEY_VERSION + " INTEGER NOT NULL, "
                   + Responses.RESPONSE_METADATA + " TEXT, "
                   + Responses.RESPONSE_EXTRAS + " TEXT, "
                   + Responses.RESPONSE_DATA + " TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.StreamData + " ("
                   + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + StreamContract.Streams.STREAM_ID + " TEXT NOT NULL, "
                   + StreamContract.Streams.STREAM_VERSION + " INTEGER NOT NULL, "
                   + StreamContract.Streams.USERNAME + " TEXT NOT NULL, "
                   + StreamContract.Streams.STREAM_METADATA + " TEXT, "
                   + StreamContract.Streams.STREAM_DATA + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Ohmlets);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Surveys);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Responses);
        onCreate(db);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Ohmlets);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Surveys);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Responses);
        onCreate(db);
    }
}
