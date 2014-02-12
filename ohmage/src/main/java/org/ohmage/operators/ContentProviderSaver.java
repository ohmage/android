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

package org.ohmage.operators;

import android.content.ContentValues;
import android.net.Uri;

import org.ohmage.app.Ohmage;

import rx.util.functions.Action1;

/**
 * Saves an item to the ohmage content provider
 */
public class ContentProviderSaver implements Action1<ContentProviderSaver.Savable> {
    private static final String TAG = ContentProviderSaver.class.getSimpleName();

    @Override public void call(Savable savable) {
        Ohmage.app().getContentResolver().insert(savable.getUrl(), savable.toContentValues());
    }

    public static interface Savable {
        ContentValues toContentValues();

        Uri getUrl();
    }
}