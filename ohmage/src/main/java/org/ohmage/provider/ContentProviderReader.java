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

import android.database.Cursor;

import com.google.gson.Gson;

import org.ohmage.app.Ohmage;

import javax.inject.Inject;

/**
 * Created by cketcham on 2/14/14.
 */
public class ContentProviderReader {
    @Inject Gson gson;

    public ContentProviderReader() {
        Ohmage.app().getApplicationGraph().inject(this);
    }

    public void read(Readable readable, Cursor cursor) {
        readable.read(this, cursor);
    }

    public Gson gson() {
        return gson;
    }

    public static interface Readable {
        void read(ContentProviderReader reader, Cursor cursor);
    }
}
