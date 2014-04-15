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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.ohmage.app.R;
import org.ohmage.app.Util;
import org.ohmage.dagger.InjectedDialogFragment;
import org.ohmage.fragments.StreamsFragment.PackageIntentReceiver;
import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.models.ApkSet;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Stream;
import org.ohmage.models.Stream.RemoteApp;
import org.ohmage.models.Survey;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.OhmageContract.Ohmlets;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * A Dialog which can be shown for a set of dependencies that should be installed. This dialog
 * can be given a set of applications explicitly via {@link #getInstance(org.ohmage.models.ApkSet)}
 * or {@link #getInstance(org.ohmage.models.ApkSet, boolean)}. Or a uri for an ohmlet can be given
 * {@link #getInstance(android.net.Uri)}. All dependencies will be shown in a ViewPager.
 */
public class InstallDependenciesDialog extends InjectedDialogFragment
        implements LoaderCallbacks<Cursor> {

    @Inject Gson gson;

    private static final int LOAD_OHMLET = 0;
    private static final int LOAD_STREAMS = 1;
    private static final int LOAD_SURVEYS = 2;

    private ArrayList<Stream> mStreams;
    private ArrayList<Survey> mSurveys;
    private ApkSet mAppItems;

    private boolean mFinishIfNotResolved;

    private Ohmlet mOhmlet;

    private Uri uri;

    private String ohmletId;

    private PackageIntentReceiver mPackageObserver;

    private ViewPager mPager;
    private DependencyPagerAdapter mFragmentAdapter;

    /**
     * Create an instance of this Dialog
     *
     * @param apps
     * @param finishIfNotResolved if true, the activity will be finished if the dialog is dismissed
     *                            before all the applications are installed
     * @return
     */
    public static InstallDependenciesDialog getInstance(ApkSet apps, boolean finishIfNotResolved) {
        InstallDependenciesDialog fragment = getInstance(apps);
        fragment.setFinishIfNotResolved(finishIfNotResolved);
        return fragment;
    }

    public static InstallDependenciesDialog getInstance(ApkSet apps) {
        InstallDependenciesDialog fragment = new InstallDependenciesDialog();
        fragment.setAppItems(apps);
        return fragment;
    }

    public static InstallDependenciesDialog getInstance(Uri uri) {
        InstallDependenciesDialog fragment = new InstallDependenciesDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable("uri", uri);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uri = getArguments().getParcelable("uri");
            if (uri != null) {
                ohmletId = uri.getLastPathSegment();
            }
        }

        setRetainInstance(true);
    }

    @Override public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mFinishIfNotResolved) {
            for (RemoteApp app : mAppItems) {
                if (!app.isInstalled(getActivity())) {
                    getActivity().finish();
                    return;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        // Work around for bug: http://code.google.com/p/android/issues/detail?id=17423
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }
        super.onDestroyView();
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);

        mPackageObserver = new PackageIntentReceiver(activity) {
            @Override public void onReceive(Context context, Intent intent) {
                mFragmentAdapter.cleanUpApks(getActivity());
                mFragmentAdapter.notifyDataSetChanged();
            }
        };
    }

    @Override public void onDetach() {
        super.onDetach();
        if (mPackageObserver != null) {
            mPackageObserver.unregister();
            mPackageObserver = null;
        }
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mAppItems == null) {
            getLoaderManager().initLoader(LOAD_OHMLET, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_install_depepndencies_dialog, container,
                false);

        getDialog().setTitle("Install Dependencies");

        mPager = (ViewPager) rootView.findViewById(R.id.pager);
        mFragmentAdapter = new DependencyPagerAdapter(getChildFragmentManager(), mAppItems);
        mPager.setAdapter(mFragmentAdapter);

        return rootView;
    }

    private void setFinishIfNotResolved(boolean finish) {
        mFinishIfNotResolved = finish;
    }

    public void setOhmlet(Ohmlet ohmlet) {
        mOhmlet = ohmlet;
    }

    private void updateItems() {

        if (mSurveys != null && mStreams != null) {
            ApkSet apps = ApkSet.fromStreams(mStreams);
            apps.addAll(ApkSet.fromSurveys(mSurveys));
            mFragmentAdapter.replaceItems(apps);
        }
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        SelectParamBuilder select = new SelectParamBuilder();

        switch (i) {
            case LOAD_OHMLET:
                return new CursorLoader(getActivity(), Ohmlets.getUriForOhmletId(ohmletId),
                        Ohmlets.DEFAULT_PROJECTION, null, null, null);
            case LOAD_STREAMS:
                for (Stream stream : mOhmlet.streams) {
                    select.or(Streams.STREAM_ID, stream.schemaId);
                }
                return new CursorLoader(getActivity(), Streams.CONTENT_URI, new String[]{
                        Streams.STREAM_ID, Streams.STREAM_VERSION, Streams.STREAM_NAME,
                        Streams.STREAM_DESCRIPTION, Streams.STREAM_APP}, select.buildSelection(),
                        select.buildParams(), null
                );
            case LOAD_SURVEYS:
                for (Survey survey : mOhmlet.surveys) {
                    select.or(Surveys.SURVEY_ID, survey.schemaId);
                }
                return new CursorLoader(getActivity(), Surveys.CONTENT_URI, new String[]{
                        Surveys.SURVEY_ID, Surveys.SURVEY_VERSION, Surveys.SURVEY_NAME,
                        Surveys.SURVEY_DESCRIPTION, Surveys.SURVEY_ITEMS}, select.buildSelection(),
                        select.buildParams(), null
                );
        }

        return null;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case LOAD_OHMLET:
                if (cursor.moveToFirst()) {
                    Ohmlet ohmlet = new Ohmlet();
                    new ContentProviderReader().read(ohmlet, cursor);
                    setOhmlet(ohmlet);
                    getLoaderManager().initLoader(LOAD_STREAMS, null, this);
                    getLoaderManager().initLoader(LOAD_SURVEYS, null, this);
                } else {
                    // No ohmlet to show?
                    dismiss();
                }
                break;
            case LOAD_STREAMS:
                mStreams = new ArrayList<Stream>();
                while (cursor.moveToNext()) {
                    Stream stream = new Stream();
                    stream.read(gson, cursor);
                    mStreams.add(stream);
                }
                updateItems();
                break;
            case LOAD_SURVEYS:
                mSurveys = new ArrayList<Survey>();
                while (cursor.moveToNext()) {
                    Survey survey = new Survey();
                    survey.read(gson, cursor);
                    mSurveys.add(survey);
                }
                updateItems();
                break;
        }
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mOhmlet = null;
        mStreams = null;
        mSurveys = null;
    }

    public void setAppItems(ApkSet appItems) {
        mAppItems = appItems;
    }

    private class DependencyPagerAdapter extends FragmentPagerAdapter {

        private final ApkSet apkSet;
        private final ArrayList<RemoteApp> oldApks = new ArrayList<RemoteApp>();
        private final ArrayList<RemoteApp> mApks;

        public DependencyPagerAdapter(FragmentManager fm, ApkSet apks) {
            super(fm);
            apkSet = apks;
            mApks = new ArrayList<RemoteApp>(apkSet);
        }

        @Override public int getCount() {
            return mApks.size();
        }

        @Override public Fragment getItem(int position) {
            return DependencyFragment.getInstance(mApks.get(position));
        }

        public void cleanUpApks(Context context) {
            apkSet.clearInstalled(context);
            oldApks.clear();
            oldApks.addAll(mApks);
            mApks.clear();
            mApks.addAll(new ArrayList<RemoteApp>(apkSet));
        }

        @Override public int getItemPosition(Object object) {
            RemoteApp app = ((DependencyFragment) object).getApp();
            int newPosition = mApks.indexOf(app);
            if (newPosition == oldApks.indexOf(app)) {
                return POSITION_UNCHANGED;
            } else if (newPosition != -1) {
                return newPosition;
            }
            return POSITION_NONE;
        }

        public void replaceItems(ApkSet apps) {
            oldApks.clear();
            mAppItems = apps;
            mApks.clear();
            mApks.addAll(mAppItems);
            notifyDataSetChanged();
        }
    }

    public static class DependencyFragment extends Fragment {

        private RemoteApp mApp;

        public static DependencyFragment getInstance(RemoteApp app) {
            DependencyFragment fragment = new DependencyFragment();
            fragment.setApp(app);
            return fragment;
        }

        private void setApp(RemoteApp app) {
            mApp = app;
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dependency_fragment, container, false);

            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(mApp.getAppName());

            Button install = (Button) view.findViewById(R.id.install);
            install.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    if (!Util.safeStartActivity(getActivity(), mApp.installIntent())) {
                        Toast.makeText(getActivity(), getString(R.string.no_app_to_install_error),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            return view;
        }

        public RemoteApp getApp() {
            return mApp;
        }
    }
}