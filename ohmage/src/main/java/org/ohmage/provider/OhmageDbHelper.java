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
import org.ohmage.streams.StreamContract.Streams;

public class OhmageDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ohmage.db";

    private static final int DB_VERSION = 5;

    public static final String SQL_AND = " AND %s='%s'";

    public interface Tables {
        static final String Ohmlets = "ohmlets";
        static final String Streams = "streams";
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
                   + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + Streams.STREAM_ID + " TEXT NOT NULL, "
                   + Streams.STREAM_VERSION + " INTEGER NOT NULL, "
                   + Streams.USERNAME + " TEXT NOT NULL, "
                   + Streams.STREAM_METADATA + " TEXT, "
                   + Streams.STREAM_DATA + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Ohmlets);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        onCreate(db);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Ohmlets);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        onCreate(db);
    }
}
