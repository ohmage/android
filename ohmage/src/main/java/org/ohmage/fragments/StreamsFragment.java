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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.gson.Gson;

import org.ohmage.app.R;
import org.ohmage.models.Stream;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.streams.StreamContract;
import org.ohmage.widget.ViewHolder;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        setEmptyText("No streams");

        setGridShown(false);

        ListAdapter adapter = new InstalledStreamsAdapter(getActivity());
        setListAdapter(adapter);
    }

    @Override
    public InstalledStreamsAdapter getListAdapter() {
        return (InstalledStreamsAdapter) super.getListAdapter();
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
        PackageManager pm = getActivity().getPackageManager();
        String packageName = app.mInfo.packageName;
        Intent intent;
        List<ResolveInfo> activities;

        // Check for the new action configure
        intent = new Intent(StreamContract.ACTION_CONFIGURE).setPackage(packageName);
        activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activities != null && !activities.isEmpty()) {
            return intent;
        }

        // Check for the old action configure
        intent = new Intent("org.ohmage.probes.ACTION_CONFIGURE").setPackage(packageName)
                .setDataAndType(null, "*/*");
        activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activities != null && !activities.isEmpty()) {
            return intent;
        }

        // Check for a main activity
        intent = new Intent(Intent.ACTION_MAIN).setPackage(packageName);
        activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activities != null && !activities.isEmpty()) {
            return intent;
        }

        // Any activity will work...
        intent = new Intent().setPackage(packageName);
        activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activities != null && !activities.isEmpty()) {
            return intent;
        }

        return null;
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getActivity(), Streams.CONTENT_URI, new String[]{
                Streams.STREAM_ID, Streams.STREAM_VERSION, Streams.STREAM_NAME,
                Streams.STREAM_DESCRIPTION, Streams.STREAM_APP}, null, null, null);
        mPackageObserver = new PackageIntentReceiver(loader);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<Stream> streams = new ArrayList<Stream>();
        while(data.moveToNext()) {
            Stream stream = new Stream();
            stream.read(gson, data);
            streams.add(stream);
        }
        getListAdapter().swapStreams(streams);
        if(!streams.isEmpty())
            setGridShown(true);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        if (mPackageObserver != null) {
            loader.getContext().unregisterReceiver(mPackageObserver);
            mPackageObserver = null;
        }

        getListAdapter().swapStreams(null);
    }

    public static class InstalledStreamsAdapter extends BaseAdapter {

        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_APK = 1;
        private static final int ITEM_VIEW_TYPE_STREAM = 2;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private ArrayList<AppEntry> mInstalled;
        private ArrayList<Stream> mNotInstalled;

        public InstalledStreamsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInstalled = new ArrayList<AppEntry>();
            mNotInstalled = new ArrayList<Stream>();
        }

        @Override public int getCount() {
            return mInstalled.size() + (mInstalled.isEmpty() ? 0 : 1)
                   + mNotInstalled.size() + (mNotInstalled.isEmpty() ? 0 : 1);
        }

        @Override public Object getItem(int position) {
            switch(getItemViewType(position)) {
                case ITEM_VIEW_TYPE_STREAM:
                    return mNotInstalled.get(position-1);
                case ITEM_VIEW_TYPE_APK:
                    return mInstalled.get(position - mNotInstalled.size() - 1 - (mNotInstalled.isEmpty() ? 0 : 1));
            }
            return null;
        }

        @Override public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0) {
                if(mNotInstalled.isEmpty() && mInstalled.isEmpty()) {
                    return -1;
                } else {
                    return ITEM_VIEW_TYPE_HEADER;
                }
            } else if(position < mNotInstalled.size() + 1) {
                return ITEM_VIEW_TYPE_STREAM;
            } else if(!mNotInstalled.isEmpty() && position == mNotInstalled.size() + 1) {
                return ITEM_VIEW_TYPE_HEADER;
            } else {
                return ITEM_VIEW_TYPE_APK;
            }
        }

        public int getViewTypeCount() {
            return 3;
        }

        private View newView(int position, ViewGroup parent) {
            switch(getItemViewType(position)) {
                case ITEM_VIEW_TYPE_STREAM:
                    return mInflater.inflate(R.layout.list_item_stream, parent, false);
                case ITEM_VIEW_TYPE_APK:
                    return mInflater.inflate(R.layout.list_item_stream_apk, parent, false);
                case ITEM_VIEW_TYPE_HEADER:
                    return new TextView(mContext);
            }
            return null;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = newView(position, parent);
            }

            Object item = getItem(position);
            final Stream stream;
            if (item instanceof Stream) {
                stream = (Stream) item;
            } else if (item instanceof AppEntry) {
                stream = ((AppEntry) item).getStream();
            } else {
                stream = null;
            }

            if (stream != null) {
                TextView title = ViewHolder.get(convertView, R.id.name);
                TextView description = ViewHolder.get(convertView, R.id.description);

                title.setText(stream.name);
                description.setText(stream.description);
            }

            switch(getItemViewType(position)) {
                case ITEM_VIEW_TYPE_STREAM:
                    Button button = ViewHolder.get(convertView, R.id.action);
                    button.setOnClickListener(new OnClickListener() {
                        @Override public void onClick(View v) {
                            mContext.startActivity(stream.app.installIntent());
                        }
                    });
                    break;
                case ITEM_VIEW_TYPE_APK:
                    ImageView icon = ViewHolder.get(convertView, R.id.icon);
                    TextView label = ViewHolder.get(convertView, R.id.app_label);
                    label.setText(((AppEntry) item).getLabel());
                    icon.setImageDrawable(((AppEntry) item).getIcon());

                    break;
                case ITEM_VIEW_TYPE_HEADER:
                    if(position == 0 && !mNotInstalled.isEmpty()) {
                        ((TextView) convertView).setText("Required");
                    } else {
                        ((TextView) convertView).setText("Installed");
                    }
                    break;
            }

            return convertView;
        }

        public void swapStreams(ArrayList<Stream> streams) {

            mInstalled.clear();
            mNotInstalled.clear();

            if(streams != null) {
                for (Stream stream : streams) {
                    try {
                        mInstalled.add(new AppEntry(getContext(), stream));
                    } catch (NameNotFoundException e) {
                        mNotInstalled.add(stream);
                    }
                }
            }

            notifyDataSetChanged();
        }

        public Context getContext() {
            return mContext;
        }

//
//        public static class Holder {
//            TextView name;
//            TextView organizer;
//            TextView description;
//            TextView response_count;
//            Button action;
//        }
//
//        public InstalledStreamsAdapter(Context context) {
//            super(context, 0);
//            Ohmage.app().getApplicationGraph().inject(this);
//            mResource = R.layout.list_item_ohmlet;
//            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        }
//
//        getI
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View view = null;
//
//            Holder holder;
//            if (convertView == null) {
//                view = mInflater.inflate(mResource, parent, false);
//                holder = new Holder();
//                holder.name = (TextView) view.findViewById(R.id.name);
//                holder.organizer = (TextView) view.findViewById(R.id.organizer);
//                holder.description = (TextView) view.findViewById(R.id.description);
//                holder.response_count = (TextView) view.findViewById(R.id.response_count);
//                holder.action = (Button) view.findViewById(R.id.action);
//                view.setTag(holder);
//            } else {
//                view = convertView;
//                holder = (Holder) view.getTag();
//            }
//
//            Stream item = getItem(position);
//            holder.name.setText(item.name);
////            holder.organizer.setText(item.organizer.fullName);
//            holder.description.setText(item.description);
////            holder.response_count.setText(String.valueOf(item.responseCount));
////            holder.action.setText(item.isParticipant ? "Leave" : "Join");
//            return view;
//        }
    }



    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final Context mContext;
        private final PackageInfo mInfo;
        private final File mApkFile;
        private final Stream mStream;
        private CharSequence mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(Context context, Stream stream) throws NameNotFoundException {
            mContext = context;
            mStream = stream;
            mInfo = stream.app.getPackageInfo(context, 0);
            mApkFile = new File(mInfo.applicationInfo.sourceDir);
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.applicationInfo.loadIcon(mContext.getPackageManager());
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.applicationInfo.loadIcon(mContext.getPackageManager());
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }

        public CharSequence getLabel() {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    mLabel = mInfo.applicationInfo.loadLabel(mContext.getPackageManager());
                    mLabel = mLabel != null ? mLabel : mInfo.packageName;
                }
            }

            return mLabel;
        }

        @Override
        public String toString() {
            return mLabel.toString();
        }

        public Stream getStream() {
            return mStream;
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };

    public static final Comparator<Stream> STREAM_COMPARATOR = new Comparator<Stream>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(Stream object1, Stream object2) {
            return sCollator.compare(object1.name, object2.name);
        }
    };

    /**
     * Helper class to look for interesting changes to the installed apps so
     * that the loader can be updated.
     */
    public static class PackageIntentReceiver extends BroadcastReceiver {
        private final Loader mLoader;

        public PackageIntentReceiver(Loader loader) {
            mLoader = loader;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mLoader.getContext().registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            mLoader.onContentChanged();
        }
    }
}