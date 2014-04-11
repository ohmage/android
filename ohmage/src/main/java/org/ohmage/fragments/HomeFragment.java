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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.ohmage.app.R;
import org.ohmage.auth.AuthUtil;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Surveys;

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

        ListAdapter adapter = new SurveyAdapter(getActivity(), null);
        setListAdapter(adapter);

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
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        if (cursor != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, OhmageContract.Surveys
                    .getUriForSurveyIdVersion(cursor.getString(0), cursor.getInt(1))));
        }
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), OhmageContract.Surveys.CONTENT_URI, new String[]{
                Surveys.SURVEY_ID, Surveys.SURVEY_VERSION, Surveys.SURVEY_NAME,
                Surveys.SURVEY_DESCRIPTION}, null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        getListAdapter().changeCursor(data);
        if(data.getCount() > 0)
            setGridShown(true);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        getListAdapter().changeCursor(null);
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

    public static class SurveyAdapter extends CursorAdapter {

        private final int mResource;

        private final LayoutInflater mInflater;

        public static class Holder {
            TextView name;
            TextView description;
            Button action;
        }

        public SurveyAdapter(Context context, Cursor c) {
            super(context, c, false);
            mResource = R.layout.list_item_survey;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return mInflater.inflate(mResource, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {

            Holder holder = (Holder) view.getTag();
            if (holder == null) {
                holder = new Holder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.action = (Button) view.findViewById(R.id.action);
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.action.setVisibility(View.GONE);
                view.setTag(holder);
            }

            holder.name.setText(cursor.getString(2));
            holder.description.setText(cursor.getString(3));
//            holder.action.setText(item.isParticipant ? "Leave" : "Join");
        }

        /**
         * Swap in a new Cursor, returning the old Cursor.  Unlike
         * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
         * closed.
         *
         * @param newCursor The new cursor to be used.
         * @return Returns the previously set Cursor, or null if there wasa not one.
         * If the given new Cursor is the same instance is the previously set
         * Cursor, null is also returned.
         */
        @Override
        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == mCursor) {
                return null;
            }
            Cursor oldCursor = mCursor;
            if (oldCursor != null) {
                if (mChangeObserver != null) oldCursor.unregisterContentObserver(mChangeObserver);
                if (mDataSetObserver != null) oldCursor.unregisterDataSetObserver(mDataSetObserver);
            }
            mCursor = newCursor;
            if (newCursor != null) {
                if (mChangeObserver != null) newCursor.registerContentObserver(mChangeObserver);
                if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);
                mRowIDColumn = newCursor.getColumnIndexOrThrow(getIdColumnName());
                mDataValid = true;
                // notify the observers about the new cursor
                notifyDataSetChanged();
            } else {
                mRowIDColumn = -1;
                mDataValid = false;
                // notify the observers about the lack of a data set
                notifyDataSetInvalidated();
            }
            return oldCursor;
        }

        protected String getIdColumnName() {
            return Surveys.SURVEY_ID;
        }
    }
}