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

import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.operators.ContentProviderStateSync.Syncable;
import org.ohmage.provider.OhmageContract;

/**
 * Basic Stream class. This class can hold a definition of a stream, and optionally the data and
 * metadata associated with a single point.
 */
public class Stream implements Savable, Syncable {
    public String schemaId;

    public long schemaVersion;

    public Stream(String schemaId, long schemaVersion) {
        this.schemaId = schemaId;
        this.schemaVersion = schemaVersion;
    }

    public Stream() {
    }

    @Override public ContentValues toContentValues(ContentProviderSaver saver) {
        ContentValues values = new ContentValues();
        values.put(OhmageContract.Streams.STREAM_ID, schemaId);
        values.put(OhmageContract.Streams.STREAM_VERSION, schemaVersion);
        return values;
    }

    @Override public Uri getUrl() {
        return OhmageContract.Streams.CONTENT_URI;
    }

    @Override public SelectParamBuilder select() {
        SelectParamBuilder select = new SelectParamBuilder();
        select.and(OhmageContract.Streams.STREAM_ID, schemaId);
        select.and(OhmageContract.Streams.STREAM_VERSION, schemaVersion);
        return select;
    }
}
