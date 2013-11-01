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
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.models.Ohmlets;

import javax.inject.Inject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Shows search results for ohmlets from server
 * TODO: figure out how to cancel a request?
 */
public class OhmletsSearchFragment extends OhmletsGridFragment
        implements SearchView.OnQueryTextListener, Callback<Ohmlets> {

    @Inject OhmageService ohmageService;

    private SearchView mSearchView;
    private String mSavedQuery;
    private boolean mSavedFocus = true;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ohmageService.searchOhmlets((String) msg.obj, OhmletsSearchFragment.this);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setGridShown(false);

        if (savedInstanceState != null) {
            mSavedQuery = savedInstanceState.getString("query");
            mSavedFocus = savedInstanceState.getBoolean("focus");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", mSearchView.getQuery().toString());
        outState.putBoolean("focus", mSearchView.hasFocus());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconifiedByDefault(false);

        mSearchView.setQuery(mSavedQuery, false);
        if (mSavedFocus) {
            mSearchView.setIconified(false);
            mSearchView.requestFocus();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        // When the submit button is pressed, we should hide the keyboard
        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        mSearchView.clearFocus();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        handler.removeMessages(0);
        if (TextUtils.isEmpty(s)) {
            getListAdapter().clear();
        } else {
            Message msg = handler.obtainMessage(0, s);
            handler.sendMessageDelayed(msg, 200);
            setGridShown(!getListAdapter().isEmpty());
        }
        return true;
    }

    @Override
    public void success(final Ohmlets ohmlets, Response response) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                setGridShown(true);
                getListAdapter().replace(ohmlets);
            }
        });
    }

    @Override
    public void failure(RetrofitError retrofitError) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
