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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.operators.ContentProviderStateSync.Syncable;
import org.ohmage.provider.ContentProviderReader.Readable;
import org.ohmage.provider.OhmageContract;

/**
 * Basic Stream class. This class can hold a definition of a stream, and optionally the data and
 * metadata associated with a single point.
 */
public class Stream implements Savable, Readable, Syncable {
    public String schemaId;

    public long schemaVersion;

    @SerializedName("apps")
    public RemoteApp app;

    public String name;

    public String description;

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
        values.put(OhmageContract.Streams.STREAM_NAME, name);
        values.put(OhmageContract.Streams.STREAM_DESCRIPTION, description);
        values.put(OhmageContract.Streams.STREAM_APP, saver.gson().toJson(app));
        return values;
    }

    @Override public Uri getUrl() {
        return OhmageContract.Streams.CONTENT_URI;
    }

    @Override public void onSaved() {

    }

    @Override public SelectParamBuilder select() {
        SelectParamBuilder select = new SelectParamBuilder();
        select.and(OhmageContract.Streams.STREAM_ID, schemaId);
        select.and(OhmageContract.Streams.STREAM_VERSION, schemaVersion);
        return select;
    }

    @Override
    public void read(Gson gson, Cursor cursor) {
        schemaId = cursor.getString(0);
        schemaVersion = cursor.getLong(1);
        name = cursor.getString(2);
        description = cursor.getString(3);
        app = gson.fromJson(cursor.getString(4), RemoteApp.class);
    }

    public static class RemoteApp {
        @Expose
        public Android android;

        public boolean isInstalled(Context context) {
            if (android == null) {
                return false;
            }
            try {
                PackageInfo info = getPackageInfo(context, 0);
                return info != null && info.versionCode >= android.versionCode;
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        @Nullable
        public Intent installIntent() {
            if (android == null) {
                return null;
            }
            return new Intent(Intent.ACTION_VIEW, Uri.parse(android.appUri));
        }

        @Nullable
        public PackageInfo getPackageInfo(Context context, int flags) throws NameNotFoundException {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                return pm.getPackageInfo(android.packageName, flags);
            }
            return null;
        }

        @Nullable
        public String getPackageName() {
            return android != null ? android.packageName : null;
        }

        @Nullable
        public String getAppName() {
            if (android == null) {
                return null;
            }
            return android.appName == null ? android.packageName : android.appName;
        }

        public int getVersionCode() {
            return android != null ? android.versionCode : -1;
        }

        public boolean appDefinitionExists() {
            return android != null;
        }

        public static class Android {
            public String appUri;

            public String authorizationUri;

            public String appName;

            @SerializedName("package")
            public String packageName;

            @SerializedName("version")
            public int versionCode;
        }

        @Override public boolean equals(Object o) {
            if(!(o instanceof RemoteApp)) {
                return false;
            } else {
                RemoteApp other = (RemoteApp) o;
                return appDefinitionExists() == other.appDefinitionExists() &&
                       (getPackageName() == null ? other.getPackageName() == null :
                        getPackageName().equals(other.getPackageName()));
            }
        }
    }
}
