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

package org.ohmage.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.operators.ContentProviderStateSync.Syncable;
import org.ohmage.prompts.Prompt;
import org.ohmage.provider.ContentProviderReader.Readable;
import org.ohmage.provider.OhmageContract;

import java.util.ArrayList;

/**
 * Created by cketcham on 12/18/13.
 */
public class Survey implements Savable, Readable, Syncable {
    public SchemaId schemaId;

    public String name;
    public String description;

    public ArrayList<Prompt> surveyItems;

    @Override public ContentValues toContentValues(ContentProviderSaver saver) {
        ContentValues values = new ContentValues();
        values.put(OhmageContract.Surveys.SURVEY_ID, schemaId.toString());
        values.put(OhmageContract.Surveys.SURVEY_VERSION, schemaId.getVersion());
        values.put(OhmageContract.Surveys.SURVEY_ITEMS, saver.gson().toJson(surveyItems));
        values.put(OhmageContract.Surveys.SURVEY_NAME, name);
        values.put(OhmageContract.Surveys.SURVEY_DESCRIPTION, description);
        return values;
    }

    @Override public Uri getUrl() {
        return OhmageContract.Surveys.CONTENT_URI;
    }

    @Override public void onSaved() {

    }

    @Override public SelectParamBuilder select() {
        SelectParamBuilder select = new SelectParamBuilder();
        select.and(OhmageContract.Surveys.SURVEY_ID, schemaId.toString());
        select.and(OhmageContract.Surveys.SURVEY_VERSION, schemaId.getVersion());
        return select;
    }

    public void read(Gson gson, Cursor cursor) {
        schemaId = new SchemaId(cursor.getString(0), cursor.getString(1));
        surveyItems = gson.fromJson(cursor.getString(2), new TypeToken<ArrayList<Prompt>>() {
        }.getType());
        name = cursor.getString(3);
        description = cursor.getString(4);
    }
}
