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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.ohmage.app.R;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Surveys;

/**
 * Created by cketcham on 7/31/13.
 */
public class HomeFragment extends GridFragment implements LoaderCallbacks<Cursor> {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No surveys");

        ListAdapter adapter = new SurveyAdapter(getActivity(), null);
        setListAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public SurveyAdapter getListAdapter() {
        return (SurveyAdapter) super.getListAdapter();
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        Log.d("blah", "list item clicked");
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        if (cursor != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, OhmageContract.Surveys
                    .getUriForSurveyIdVersion(cursor.getString(0), cursor.getInt(1))));
        }
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), OhmageContract.Surveys.CONTENT_URI, new String[]{
                Surveys.SURVEY_ID, Surveys.SURVEY_VERSION}, null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setGridShown(true);
        getListAdapter().changeCursor(data);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        getListAdapter().changeCursor(null);
    }

    public static class SurveyAdapter extends CursorAdapter {

        private final int mResource;

        private final LayoutInflater mInflater;

        public static class Holder {
            TextView name;
            Button action;
        }

        public SurveyAdapter(Context context, Cursor c) {
            super(context, c, false);
            mResource = R.layout.list_item_ohmlet;
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
                view.setTag(holder);
            }

            holder.name.setText(cursor.getString(0));
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