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

package org.ohmage.fragments;

import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.google.gson.Gson;

import org.ohmage.app.R;
import org.ohmage.app.Util;
import org.ohmage.fragments.InstalledAppItemsAdapter.AppEntry;
import org.ohmage.models.ApkSet;
import org.ohmage.models.Stream;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.streams.StreamContract;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by cketcham on 7/31/13.
 */
public class StreamsFragment extends GridFragment implements LoaderCallbacks<Cursor> {

    private static final String TAG = StreamsFragment.class.getSimpleName();

    @Inject AccountManager am;

    @Inject Gson gson;

    private PackageIntentReceiver mPackageObserver;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_streams));

        setGridShown(false);

        ListAdapter adapter = new InstalledAppItemsAdapter(getActivity());
        setListAdapter(adapter);
    }

    @Override
    public InstalledAppItemsAdapter getListAdapter() {
        return (InstalledAppItemsAdapter) super.getListAdapter();
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        Object item = getListAdapter().getItem(position);
        if (item instanceof AppEntry) {
            Intent intent = launchStreamIntent(((AppEntry) item));
            if(intent != null) {
                startActivity(intent);
            }
        }
    }

    /**
     * Checks for multiple different intents that might launch the stream.
     */
    private Intent launchStreamIntent(AppEntry app) {
        String packageName = app.getInfo().packageName;
        Intent intent;

        // Check for the new action configure
        intent = new Intent(StreamContract.ACTION_CONFIGURE).setPackage(packageName);
        if (Util.activityExists(getActivity(), intent)) {
            return intent;
        }

        // Check for the old action configure
        intent = new Intent("org.ohmage.probes.ACTION_CONFIGURE").setPackage(packageName)
                .setDataAndType(null, "*/*");
        if (Util.activityExists(getActivity(), intent)) {
            return intent;
        }

        // Check for a main activity
        intent = new Intent(Intent.ACTION_MAIN).setPackage(packageName);
        if (Util.activityExists(getActivity(), intent)) {
            return intent;
        }

        // Any activity will work...
        intent = new Intent().setPackage(packageName);
        if (Util.activityExists(getActivity(), intent)) {
            return intent;
        }

        return null;
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getActivity(), Streams.CONTENT_URI,
                Streams.DEFAULT_PROJECTION, null, null, null);
        mPackageObserver = new LoaderPackageIntentReceiver(loader);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<Stream> streams = new ArrayList<Stream>();
        while(data.moveToNext()) {
            Stream stream = new Stream();
            stream.read(gson, data);
            streams.add(stream);
        }
        getListAdapter().swapItems(ApkSet.fromStreams(streams));
        setGridShown(true);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        if (mPackageObserver != null) {
            mPackageObserver.unregister();
            mPackageObserver = null;
        }

        getListAdapter().swapItems(null);
    }



//
//    /**
//     * Perform alphabetical comparison of application entry objects.
//     */
//    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
//        private final Collator sCollator = Collator.getInstance();
//
//        @Override
//        public int compare(AppEntry object1, AppEntry object2) {
//            return sCollator.compare(object1.getLabel(), object2.getLabel());
//        }
//    };
//
//    public static final Comparator<Stream> STREAM_COMPARATOR = new Comparator<Stream>() {
//        private final Collator sCollator = Collator.getInstance();
//
//        @Override
//        public int compare(Stream object1, Stream object2) {
//            return sCollator.compare(object1.name, object2.name);
//        }
//    };

    /**
     * Helper class to look for interesting changes to the installed apps so
     * that the loader can be updated.
     */
    public static abstract class PackageIntentReceiver extends BroadcastReceiver {
        private final Context mContext;

        public PackageIntentReceiver(Context context) {
            mContext = context;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mContext.registerReceiver(this, filter);
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }
    }

    public static class LoaderPackageIntentReceiver extends PackageIntentReceiver {
        private final Loader mLoader;

        public LoaderPackageIntentReceiver(Loader loader) {
            super(loader.getContext());
            mLoader = loader;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            mLoader.onContentChanged();
        }
    }
}