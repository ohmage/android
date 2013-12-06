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

package org.ohmage.streams;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class StreamDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "streams.db";

    private static final int DB_VERSION = 1;

    public static final String SQL_AND = " AND %s='%s'";

    public interface Tables {
        static final String Streams = "streams";
    }

    public StreamDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Streams + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + StreamContract.Streams.STREAM_ID + " TEXT NOT NULL, "
                + StreamContract.Streams.STREAM_VERSION + " INTEGER NOT NULL, "
                + StreamContract.Streams.USERNAME + " TEXT NOT NULL, "
                + StreamContract.Streams.STREAM_METADATA + " TEXT, "
                + StreamContract.Streams.STREAM_DATA + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        onCreate(db);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Streams);
        onCreate(db);
    }
}
