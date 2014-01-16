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
import android.net.Uri;

import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.provider.OhmageContract;

import java.util.ArrayList;

/**
 * Created by cketcham on 12/18/13.
 */
public class Survey implements Savable {
    public String schemaId;

    public long schemaVersion;
    public String name;

    ArrayList<Object> surveyItems;

    @Override public ContentValues toContentValues(ContentProviderSaver saver) {
        ContentValues values = new ContentValues();
        values.put(OhmageContract.Surveys.SURVEY_ID, schemaId);
        values.put(OhmageContract.Surveys.SURVEY_VERSION, schemaVersion);
        values.put(OhmageContract.Surveys.SURVEY_ITEMS, saver.gson().toJson(surveyItems));
        return values;
}

    @Override public Uri getUrl() {
        return OhmageContract.Surveys.CONTENT_URI;
    }
}
