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

import org.ohmage.app.Ohmage;
import org.ohmage.app.R;
import org.ohmage.models.Ohmlet;
import org.ohmage.widget.CompatArrayAdapter;

/**
 * Shows ohmlets from the server
 */
public class OhmletsGridFragment extends GridFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No ohmlets");

        ListAdapter adapter = new OhmletAdapter(getActivity());
        setListAdapter(adapter);
    }

    @Override
    public OhmletAdapter getListAdapter() {
        return (OhmletAdapter) super.getListAdapter();
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        Log.d("blah", "list item clicked");
    }

    public static class OhmletAdapter extends CompatArrayAdapter<Ohmlet> {

        private final int mResource;

        private final LayoutInflater mInflater;

        public static class Holder {
            TextView name;
            TextView organizer;
            TextView description;
            TextView response_count;
            Button action;
        }

        public OhmletAdapter(Context context) {
            super(context, 0);
            Ohmage.app().getApplicationGraph().inject(this);
            mResource = R.layout.list_item_ohmlet;
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
                holder.organizer = (TextView) view.findViewById(R.id.organizer);
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.response_count = (TextView) view.findViewById(R.id.response_count);
                holder.action = (Button) view.findViewById(R.id.action);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (Holder) view.getTag();
            }

            Ohmlet item = getItem(position);
            holder.name.setText(item.name);
            holder.organizer.setText(item.organizer.fullName);
            holder.description.setText(item.description);
            holder.response_count.setText(String.valueOf(item.responseCount));
            holder.action.setText(item.isParticipant ? "Leave" : "Join");
            return view;
        }
    }
}
