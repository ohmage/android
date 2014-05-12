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

import android.net.Uri;

import org.ohmage.app.Ohmage;
import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.operators.ContentProviderStateSync.Syncable;
import org.ohmage.sync.OhmageSyncAdapter;

import java.util.List;

import rx.Subscriber;
import rx.util.functions.Action1;

/**
 * Removes any items from the db that aren't in the list
 */
public class ContentProviderStateSync implements Action1<List<? extends Syncable>> {
    private static final String TAG = ContentProviderStateSync.class.getSimpleName();

    private Uri mUri;

    public ContentProviderStateSync(Uri uri, boolean isSyncAdapter) {
        mUri = uri;
        if(isSyncAdapter) {
            mUri = OhmageSyncAdapter.appendSyncAdapterParam(uri);
        }
    }

    @Override public void call(List<? extends Syncable> syncable) {
        SelectParamBuilder select = new SelectParamBuilder();
        for (Syncable s : syncable) {
            select.orSubSelect(s.select());
        }

        Ohmage.app().getContentResolver().delete(mUri, select.negate().buildSelection(),
                select.buildParams());
    }

    public static interface Syncable {
        SelectParamBuilder select();

        Uri getUrl();
    }

    public static class ContentProviderStateSyncSubscriber extends
            Subscriber<List<? extends Syncable>> {

        private final Uri mUri;
        private final boolean mIsSyncAdapter;

        public ContentProviderStateSyncSubscriber(Uri uri, boolean isSyncAdapter) {
            mUri = uri;
            mIsSyncAdapter = isSyncAdapter;
        }

        @Override public void onCompleted() {

        }

        @Override public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override public void onNext(List<? extends Syncable> args) {
            new ContentProviderStateSync(mUri, mIsSyncAdapter).call(args);
        }
    }
}