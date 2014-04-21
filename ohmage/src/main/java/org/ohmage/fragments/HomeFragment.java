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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ohmage.app.R;
import org.ohmage.auth.AuthUtil;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.reminders.base.ReminderContract.Reminders;
import org.ohmage.widget.ViewHolder;

import java.util.ArrayList;

import javax.inject.Inject;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

/**
 * Created by cketcham on 7/31/13.
 */
public class HomeFragment extends GridFragment implements LoaderCallbacks<Cursor>,
        OnRefreshListener, SyncStatusObserver {

    @Inject AccountManager am;

    private static final String TAG = HomeFragment.class.getSimpleName();
    private PullToRefreshLayout mPullToRefreshLayout;

    private Object syncObserverHandle;
    private Handler mHandler = new Handler();
    private Runnable mRefreshCompleteRunnable = new Runnable() {
        @Override public void run() {
            if(getView() != null) {
                setEmptyText("No Surveys");
                mPullToRefreshLayout.setRefreshComplete();
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public void onResume() {
        super.onResume();

        syncObserverHandle = getActivity().getContentResolver().addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);
    }

    @Override public void onPause() {
        super.onPause();
        if(syncObserverHandle != null) {
            getActivity().getContentResolver().removeStatusChangeListener(syncObserverHandle);
            syncObserverHandle = null;
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRefreshCompleteRunnable);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No surveys");

        setGridShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.refreshable_grid_layout, container, false);

        // Now find the PullToRefreshLayout to setup
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(getActivity())
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                .theseChildrenArePullable(R.id.grid, R.id.internalEmpty)
                .useViewDelegate(TextView.class, new ViewDelegate() {
                    @Override public boolean isReadyForPull(View view, float v, float v2) {
                        return true;
                    }
                })
                .useViewDelegate(GridView.class, new AbsListViewDelegate())
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);

        return view;
    }

    @Override
    public SurveyAdapter getListAdapter() {
        return (SurveyAdapter) super.getListAdapter();
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        Cursor cursor = getListAdapter().getItem(position).getData();
        if (cursor != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, OhmageContract.Surveys
                    .getUriForSurveyIdVersion(cursor.getString(0), cursor.getInt(1))));
        }
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), OhmageContract.Surveys.CONTENT_URI, new String[]{
                Surveys.SURVEY_ID, Surveys.SURVEY_VERSION, Surveys.SURVEY_NAME,
                Surveys.SURVEY_DESCRIPTION, Surveys.SURVEY_PENDING_TIME,
                Surveys.SURVEY_PENDING_TIMEZONE}, null, null,
                Surveys.SURVEY_PENDING_TIME + " desc"
        );
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        ArrayList<Item> items = new ArrayList<Item>();
        boolean pivot = true;
        data.moveToPosition(-1);
        while (data.moveToNext()) {
            if (items.isEmpty() && data.getLong(4) != Reminders.NOT_PENDING) {
                pivot = false;
                items.add(new Item("Pending"));
            } else if (!items.isEmpty() && data.getLong(4) == Reminders.NOT_PENDING && !pivot) {
                pivot = true;
                items.add(new Item("Surveys"));
            }
            items.add(new Item(data, 0));
        }

        SurveyAdapter adapter = getListAdapter();
        if (adapter == null) {
            adapter = new SurveyAdapter(getActivity(), items);
            setListAdapter(adapter);
        } else {
            adapter.setData(items);
        }

        setGridShown(true);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        getListAdapter().setData(null);
    }

    @Override public void onRefreshStarted(View view) {
        Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        for (Account account : accounts) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            getActivity().getContentResolver()
                    .requestSync(account, OhmageContract.CONTENT_AUTHORITY, bundle);
        }
    }

    @Override public void onStatusChanged(int which) {
        Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        if(accounts.length == 0) {
            return;
        }

        ContentResolver cr = getActivity().getContentResolver();

        mHandler.removeCallbacks(mRefreshCompleteRunnable);
        if(cr.isSyncActive(accounts[0], OhmageContract.CONTENT_AUTHORITY) ||
           cr.isSyncPending(accounts[0], OhmageContract.CONTENT_AUTHORITY)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(getListAdapter().isEmpty()) {
                        mPullToRefreshLayout.setRefreshing(true);
                        setEmptyText("Please wait for Surveys to Synchronize...");
                    }
                }
            });
        } else {
            mHandler.postDelayed(mRefreshCompleteRunnable, 500);
        }
    }

    private static class Item {
        private int dataPosition = -1;

        /**
         * The index of the id field for the cursor
         */
        private final int idIdex;

        private final Cursor data;
        private final String header;

        public Item(String header) {
            this(header, null, -1);
            if (header == null) {
                throw new IllegalArgumentException("the header string can't be null");
            }
        }

        public Item(Cursor data, int idIdx) {
            this(null, data, idIdx);
            if (data == null) {
                throw new IllegalArgumentException("the cursor data shouldn't be null");
            }
        }

        private Item(String header, Cursor data, int idIdx) {
            this.header = header;
            this.data = data;
            if (data != null) {
                this.dataPosition = data.getPosition();
            } else {
                this.dataPosition = -1;
            }
            this.idIdex = idIdx;
        }

        public boolean isHeader() {
            return data == null;
        }

        public Cursor getData() {
            data.moveToPosition(dataPosition);
            return data;
        }

        public long getItemId() {
            return data == null ? header.hashCode() : getData().getString(0).hashCode();
        }
    }

    public static class SurveyAdapter extends BaseAdapter {
        private static final int HEADER = 0;
        private static final int DATA = 1;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private ArrayList<Item> mData;

        private final DateTimeFormatter mTimeFormatter;

        public SurveyAdapter(Context context, ArrayList<Item> data) {
            super();
            mContext = context;
            mData = data;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTimeFormatter = DateTimeFormat.shortTime().withZone(DateTimeZone.getDefault());
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return !getItem(position).isHeader();
        }

        public int getItemViewType(int position) {
            return getItem(position).isHeader() ? HEADER : DATA;
        }

        public int getViewTypeCount() {
            return 2;
        }

        @Override public int getCount() {
            return mData.size();
        }

        @Override public Item getItem(int position) {
            return mData.get(position);
        }

        @Override public long getItemId(int position) {
            return getItem(position).getItemId();
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Item item = getItem(position);
            if (convertView == null) {
                if (item.isHeader()) {
                    convertView = new TextView(mContext);
                } else {
                    convertView = mInflater.inflate(R.layout.list_item_local_survey, parent, false);
                }
            }

            if (item.isHeader()) {
                ((TextView) convertView).setText(item.header);
            } else {
                TextView name = ViewHolder.get(convertView, R.id.name);
                TextView description = ViewHolder.get(convertView, R.id.description);
                TextView pending = ViewHolder.get(convertView, R.id.pendingTime);

                Cursor data = item.getData();

                name.setText(data.getString(2));
                description.setText(data.getString(3));
                long pendingTime = data.getLong(4);
                String zone = data.getString(5);
                pending.setText(
                        mTimeFormatter.withZone(DateTimeZone.forID(zone)).print(pendingTime));
                pending.setVisibility(
                        pendingTime == Reminders.NOT_PENDING ? View.GONE : View.VISIBLE);
            }

            return convertView;
        }

        public void setData(ArrayList<Item> data) {
            if (data == null) {
                mData.clear();
            } else {
                mData = data;
            }
            notifyDataSetChanged();
        }
    }
}