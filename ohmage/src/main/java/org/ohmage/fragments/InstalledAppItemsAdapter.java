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

package org.ohmage.fragments;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.app.R;
import org.ohmage.app.Util;
import org.ohmage.models.ApkSet;
import org.ohmage.models.Stream.RemoteApp;
import org.ohmage.widget.ViewHolder;

import java.io.File;
import java.util.ArrayList;

/**
 * An adapter which will show all required and installed apps given an {@link org.ohmage.models.ApkSet}
 */
public class InstalledAppItemsAdapter extends BaseAdapter {

    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    private static final int ITEM_VIEW_TYPE_APK = 1;
    private static final int ITEM_VIEW_TYPE_STREAM = 2;

    private final LayoutInflater mInflater;
    private final Context mContext;
    private ArrayList<AppEntry> mInstalled;
    private ArrayList<RemoteApp> mNotInstalled;

    public InstalledAppItemsAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInstalled = new ArrayList<AppEntry>();
        mNotInstalled = new ArrayList<RemoteApp>();
    }

    @Override public int getCount() {
        return mInstalled.size() + (mInstalled.isEmpty() ? 0 : 1)
               + mNotInstalled.size() + (mNotInstalled.isEmpty() ? 0 : 1);
    }

    @Override public Object getItem(int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_STREAM:
                return mNotInstalled.get(position - 1);
            case ITEM_VIEW_TYPE_APK:
                return mInstalled.get(position - mNotInstalled.size() - 1 -
                                      (mNotInstalled.isEmpty() ? 0 : 1));
        }
        return null;
    }

    @Override public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            if (mNotInstalled.isEmpty() && mInstalled.isEmpty()) {
                return -1;
            } else {
                return ITEM_VIEW_TYPE_HEADER;
            }
        } else if (position < mNotInstalled.size() + 1) {
            return ITEM_VIEW_TYPE_STREAM;
        } else if (!mNotInstalled.isEmpty() && position == mNotInstalled.size() + 1) {
            return ITEM_VIEW_TYPE_HEADER;
        } else {
            return ITEM_VIEW_TYPE_APK;
        }
    }

    public int getViewTypeCount() {
        return 3;
    }

    private View newView(int position, ViewGroup parent) {
        switch (getItemViewType(position)) {
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
        final RemoteApp appItem;
        if (item instanceof RemoteApp) {
            appItem = (RemoteApp) item;
        } else if (item instanceof AppEntry) {
            appItem = ((AppEntry) item).getAppItem();
        } else {
            appItem = null;
        }

        if (appItem != null) {
            TextView title = ViewHolder.get(convertView, R.id.name);
            TextView description = ViewHolder.get(convertView, R.id.description);

            title.setText(appItem.getAppName());
            description.setText(appItem.getPackageName());
        }

        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_STREAM:
                Button button = ViewHolder.get(convertView, R.id.action);
                button.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!Util.safeStartActivity(getContext(), appItem.installIntent())) {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.no_app_to_install_error),
                                    Toast.LENGTH_SHORT).show();
                        }
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
                if (position == 0 && !mNotInstalled.isEmpty()) {
                    ((TextView) convertView)
                            .setText(mContext.getString(R.string.required_apps_header));
                } else {
                    ((TextView) convertView)
                            .setText(mContext.getString(R.string.installed_apps_header));
                }
                break;
        }

        return convertView;
    }

    public void swapItems(ApkSet apps) {

        mInstalled.clear();
        mNotInstalled.clear();

        if (apps != null) {
            for (RemoteApp app : apps) {
                try {
                    if (app.isInstalled(getContext())) {
                        mInstalled.add(new AppEntry(getContext(), app));
                    } else {
                        mNotInstalled.add(app);
                    }
                } catch (NameNotFoundException e) {
                    mNotInstalled.add(app);
                }
            }
        }

        notifyDataSetChanged();
    }

    public Context getContext() {
        return mContext;
    }


    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final Context mContext;
        private final PackageInfo mInfo;
        private final File mApkFile;
        private final RemoteApp mItem;
        private CharSequence mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(Context context, RemoteApp app) throws NameNotFoundException {
            mContext = context;
            mItem = app;
            mInfo = app.getPackageInfo(context, 0);
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

        public PackageInfo getInfo() {
            return mInfo;
        }

        @Override
        public String toString() {
            return mLabel.toString();
        }

        public RemoteApp getAppItem() {
            return mItem;
        }
    }
}