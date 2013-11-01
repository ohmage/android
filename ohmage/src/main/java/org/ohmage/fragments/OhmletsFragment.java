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

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.models.Ohmlets;

import javax.inject.Inject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Shows ohmlets from the server
 */
public class OhmletsFragment extends OhmletsGridFragment {

    @Inject OhmageService ohmageService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SearchView) v).setIconified(true);
                getActivity().getSupportFragmentManager().beginTransaction()
                             .replace(getId(), new OhmletsSearchFragment())
                             .addToBackStack(null)
                             .commit();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setGridShown(false);

        ohmageService.getOhmlets(new Callback<Ohmlets>() {
            @Override
            public void success(final Ohmlets ohmlets, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setGridShown(true);
                        getListAdapter().replace(ohmlets);
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
        });

    }
}
