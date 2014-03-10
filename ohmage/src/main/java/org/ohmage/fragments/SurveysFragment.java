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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.models.Survey;
import org.ohmage.models.Surveys;
import org.ohmage.widget.CompatArrayAdapter;

import javax.inject.Inject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Shows surveys from the server
 */
public class SurveysFragment extends GridFragment implements Callback<Surveys> {

    @Inject OhmageService ohmageService;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No surveys");

        ListAdapter adapter = new SurveyAdapter(getActivity());
        setListAdapter(adapter);

        ohmageService.getSurveys(this);
    }

    @Override
    public SurveyAdapter getListAdapter() {
        return (SurveyAdapter) super.getListAdapter();
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        Log.d("blah", "list item clicked");
    }

    @Override
    public void success(final Surveys surveys, Response response) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setGridShown(true);
                getListAdapter().replace(surveys);
            }
        });
    }

    @Override
    public void failure(RetrofitError retrofitError) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setGridShown(true);
                Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class SurveyAdapter extends CompatArrayAdapter<Survey> {

        private final int mResource;

        private final LayoutInflater mInflater;

        public static class Holder {
            TextView name;
            Button action;
        }

        public SurveyAdapter(Context context) {
            super(context, 0);
            mResource = R.layout.list_item_survey;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;

            Holder holder;
            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
                holder = new Holder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.action = (Button) view.findViewById(R.id.action);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (Holder) view.getTag();
            }

            Survey item = getItem(position);
            holder.name.setText(item.name);
//            holder.action.setText(item.isParticipant ? "Leave" : "Join");
            return view;
        }
    }
}
