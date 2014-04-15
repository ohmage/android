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

package org.ohmage.app;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.google.gson.Gson;

import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.fragments.GridFragment;
import org.ohmage.fragments.InstalledAppItemsAdapter;
import org.ohmage.fragments.StreamsFragment;
import org.ohmage.fragments.StreamsFragment.LoaderPackageIntentReceiver;
import org.ohmage.fragments.StreamsFragment.PackageIntentReceiver;
import org.ohmage.models.ApkSet;
import org.ohmage.models.Survey;
import org.ohmage.provider.OhmageContract.Surveys;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Shows a list of the apps for remote prompts for all surveys that need to be installed
 */
public class InstallSurveyDependencies extends InjectedActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    new InstallSurveyDependenciesFragment()).commit();
        }
    }

    public static class InstallSurveyDependenciesFragment extends GridFragment
            implements LoaderCallbacks<Cursor> {

        private static final String TAG = StreamsFragment.class.getSimpleName();

        @Inject Gson gson;

        private PackageIntentReceiver mPackageObserver;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setEmptyText(getString(R.string.no_survey_apps));

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
            // Nothing
        }

        @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader loader = new CursorLoader(getActivity(), Surveys.CONTENT_URI,
                    Surveys.DEFAULT_PROJECTION, null, null, null);
            mPackageObserver = new LoaderPackageIntentReceiver(loader);
            return loader;
        }

        @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            ArrayList<Survey> surveys = new ArrayList<Survey>();
            while (data.moveToNext()) {
                Survey survey = new Survey();
                survey.read(gson, data);
                surveys.add(survey);
            }
            getListAdapter().swapItems(ApkSet.fromSurveys(surveys));
            setGridShown(true);
        }

        @Override public void onLoaderReset(Loader<Cursor> loader) {
            if (mPackageObserver != null) {
                loader.getContext().unregisterReceiver(mPackageObserver);
                mPackageObserver = null;
            }

            getListAdapter().swapItems(null);
        }
    }

}
