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

package org.ohmage.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viewpagerindicator.CirclePageIndicator;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.prompts.AnswerablePrompt;
import org.ohmage.prompts.BasePrompt.AnswerablePromptFragment;
import org.ohmage.prompts.Prompt;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Responses;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.streams.StreamPointBuilder;
import org.ohmage.widget.PromptViewPager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by cketcham on 12/18/13.
 */
public class SurveyActivity extends InjectedActionBarActivity
        implements LoaderCallbacks<ArrayList<Prompt>>, ConnectionCallbacks,
        OnConnectionFailedListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    @Inject
    OhmageService ohmageService;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public PromptViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PromptFragmentAdapter mPagerAdapter;

    private CirclePageIndicator indicator;

    /**
     * Location client to get accurate location
     */
    private LocationClient mLocationClient;

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (PromptViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.gutter));

        //Bind the title indicator to the adapter
        indicator = (CirclePageIndicator) findViewById(R.id.titles);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpLocationClientIfNeeded();
        mLocationClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }

    private void setUpLocationClientIfNeeded() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(
                    getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener
        }
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    @Override public SurveyPromptLoader onCreateLoader(int id, Bundle args) {
        return new SurveyPromptLoader(this, getIntent().getData());
    }

    @Override public void onLoadFinished(Loader<ArrayList<Prompt>> loader, ArrayList<Prompt> data) {
        if (mPagerAdapter == null) {
            mPagerAdapter = new PromptFragmentAdapter(getSupportFragmentManager(), data);
            mPager.setAdapter(mPagerAdapter);
            indicator.setViewPager(mPager);
        }
    }

    @Override public void onLoaderReset(Loader<ArrayList<Prompt>> loader) {

    }

    public void submit() {
        try {
            ContentValues values = new ContentValues();
            values.put(Responses.SURVEY_ID, Surveys.getId(getIntent().getData()));
            values.put(Responses.SURVEY_VERSION, Surveys.getVersion(getIntent().getData()));
            values.put(Responses.RESPONSE_METADATA,
                    new StreamPointBuilder().now().withId()
                            .withLocation(mLocationClient.getLastLocation())
                            .getMetadata());
            mPagerAdapter.buildResponse(values);
            getContentResolver().insert(Responses.CONTENT_URI, values);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "There was an error saving the response", Toast.LENGTH_SHORT);
        }
    }

    @Override public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(REQUEST, new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                Log.d(TAG, "accuracy:"  + location.getAccuracy());
            }
        });  // LocationListener
    }

    @Override public void onDisconnected() {

    }

    @Override public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class PromptFragmentAdapter extends FragmentStatePagerAdapter {
        private final List<Prompt> mPrompts;

        int mLastValidPrompt = 0;

        /**
         * Keeps track of what fragments have been created
         */
        private BitSet mFragments = new BitSet();

        public PromptFragmentAdapter(FragmentManager fm, List<Prompt> prompts) {
            super(fm);
            mPrompts = prompts;
            mLastValidPrompt = mPager.getCurrentItem();
        }

        @Override
        public Fragment getItem(final int position) {
            Fragment fragment = null;
            if (position == getCount() - 1) {
                fragment = new SubmitResponseFragment();
            } else {
                fragment = mPrompts.get(position).getFragment();
                if (fragment instanceof AnswerablePromptFragment) {
                    ((AnswerablePromptFragment) fragment).setOnValidAnswerStateChangedListener(mPager);
                }
            }

            mFragments.set(position);

            return fragment;
        }

        public Fragment getObject(int position) {
            if (mFragments.get(position)) {
                return (Fragment) super.instantiateItem(null, position);
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.clear(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return mPrompts.size() + 1;
        }

        @Override
        public float getPageWidth(int position) {
            if (position == getCount() - 1)
                return 1.0f;
            return 0.1f;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // Hijack the fragment state from our parent
            if (state != null) {
                Bundle bundle = (Bundle) state;
                mFragments.clear();
                Iterable<String> keys = bundle.keySet();
                for (String key : keys) {
                    if (key.startsWith("f")) {
                        int index = Integer.parseInt(key.substring(1));
                        Fragment f = getSupportFragmentManager().getFragment(bundle, key);
                        if (f != null) {
                            mFragments.set(index, true);
                        } else {
                            Log.w(TAG, "Bad fragment at key " + key);
                        }
                    }
                }
            }
            super.restoreState(state, loader);
        }

        public int getPromptPosition(Prompt prompt) {
            return mPrompts.indexOf(prompt);
        }

        public void buildResponse(ContentValues values) throws JSONException {
            JSONObject extras = new JSONObject();
            JSONObject data = new JSONObject();
            for (int i = 0; i < getCount(); i++) {
                Fragment item = getItem(i);
                if (item instanceof AnswerablePromptFragment) {
                    //TODO: remove some casting?
                    ((AnswerablePrompt) ((AnswerablePromptFragment) item).getPrompt()).addAnswer(
                            data, extras);
                }
            }
            values.put(Responses.RESPONSE_EXTRAS, extras.toString());
            values.put(Responses.RESPONSE_DATA, data.toString());
        }
    }

    public static class BasePromptAdapterFragment extends Fragment {

        boolean mHidden = true;

        private View.OnClickListener mOnClickListener;
        private View mButtons;
        private TextView skipButton;
        private TextView okButton;

        private boolean mOkPressed = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            if (savedInstanceState != null) {
                mHidden = savedInstanceState.getBoolean("hidden", false);
                mOkPressed = savedInstanceState.getBoolean("okPressed", false);
            }
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_basic, container, false);
            ((TextView) view.findViewById(R.id.text)).setText(getPromptText());
            mButtons = view.findViewById(R.id.buttons);
            skipButton = (TextView) view.findViewById(R.id.skip);
            okButton = (TextView) view.findViewById(R.id.ok);

            okButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    mOkPressed = true;
                    onOkPressed();
                }
            });
            skipButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    mOkPressed = true;
                    onSkipPressed();
                }
            });

            if(isSkippable()) {
                skipButton.setVisibility(View.VISIBLE);
            } else {
                skipButton.setVisibility(View.GONE);
            }

            if (getOkPressed()) {
                view.findViewById(R.id.buttons).setVisibility(View.GONE);
            } else {
                updateCanContinue();
            }

            onCreatePromptView(inflater, (ViewGroup) view.findViewById(R.id.content),
                    savedInstanceState);

            return view;
        }

        protected void onSkipPressed() {

        }

        protected void onOkPressed() {

        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            view.setOnClickListener(mOnClickListener);
            setHidden(view, mHidden, 0);
        }

        protected String getPromptText() {
            return "";
        }

        protected boolean canContinue() {
            return true;
        }

        protected void updateCanContinue() {
            if(canContinue()) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }

        protected boolean isSkippable() {
            return true;
        }

        /**
         * Allow children to inflate their subview into the main view.
         *
         * @param inflater
         * @param container
         * @param savedInstanceState
         */
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        }

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("hidden", mHidden);
            outState.putBoolean("okPressed", mOkPressed);
        }

        public void setHidden(boolean hidden) {
            if(mHidden == hidden)
                return;
            setHidden(getView(), hidden, 200);
        }

        public void setHidden(boolean hidden, int duration) {
            if(mHidden == hidden)
                return;
            setHidden(getView(), hidden, duration);
        }

        protected void setHidden(View view, boolean hidden, int duration) {
            mHidden = hidden;
            if (view == null)
                return;
            AlphaAnimation aa = (hidden) ? new AlphaAnimation(1f, 0f) : new AlphaAnimation(0f, 1f);
            aa.setFillAfter(true);
            aa.setDuration(duration);
            view.startAnimation(aa);
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }

        public boolean getOkPressed() {
            return mOkPressed;
        }

        public void showButtons(int visibility) {
            if(mButtons != null)
                mButtons.setVisibility(visibility);
        }
    }

    public static class SubmitResponseFragment extends BasePromptAdapterFragment {

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.prompt_submit, container, false);
            Button submit = (Button) view.findViewById(R.id.submit);
            submit.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    ((SurveyActivity) getActivity()).submit();
                }
            });
            return view;
        }
    }

    /**
     * Static library support version of the framework's {@link android.content.CursorLoader}.
     * Used to write apps that run on platforms prior to Android 3.0.  When running
     * on Android 3.0 or above, this implementation is still used; it does not try
     * to switch to the framework's implementation.  See the framework SDK
     * documentation for a class overview.
     */
    public static class SurveyPromptLoader extends AsyncTaskLoader<ArrayList<Prompt>> {
        @Inject Gson gson;

        Uri mUri;
        String[] mProjection;
        String mSelection;
        String[] mSelectionArgs;

        Cursor mCursor;

        private ArrayList<Prompt> mPrompts;

        /* Runs on a worker thread */
        @Override
        public ArrayList<Prompt> loadInBackground() {
            Cursor cursor = getContext().getContentResolver().query(mUri, mProjection, mSelection,
                    mSelectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                mPrompts = gson.fromJson(cursor.getString(0), new TypeToken<ArrayList<Prompt>>() {
                }.getType());
                cursor.close();
            }
            return mPrompts;
        }

        /* Runs on the UI thread */
        @Override
        public void deliverResult(ArrayList<Prompt> result) {
            if (isReset()) {
                // An async query came in while the loader is stopped
                mPrompts = null;
                return;
            }
            mPrompts = result;
            if (isStarted()) {
                super.deliverResult(result);
            }
        }

        /**
         * Creates a Loader for prompt items
         */
        public SurveyPromptLoader(Context context, String surveyId, long surveyVersion) {
            super(context);
            ((SurveyActivity) context).inject(this);

            mUri = OhmageContract.Surveys.CONTENT_URI;
            mProjection = new String[]{
                    OhmageContract.Surveys.SURVEY_ITEMS
            };
            mSelection = OhmageContract.Surveys.SURVEY_ID + "=? AND " +
                         OhmageContract.Surveys.SURVEY_VERSION + "=?";
            mSelectionArgs = new String[]{
                    surveyId, String.valueOf(surveyVersion)
            };
        }

        public SurveyPromptLoader(Context context, Uri surveyUri) {
            super(context);
            ((SurveyActivity) context).inject(this);

            mUri = surveyUri;
            mProjection = new String[]{
                    OhmageContract.Surveys.SURVEY_ITEMS
            };
        }

        /**
         * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
         * will be called on the UI thread. If a previous load has been completed and is still valid
         * the result may be passed to the callbacks immediately.
         * <p/>
         * Must be called from the UI thread
         */
        @Override
        protected void onStartLoading() {
            if (mPrompts != null) {
                deliverResult(mPrompts);
            }
            if (takeContentChanged() || mCursor == null) {
                forceLoad();
            }
        }

        /**
         * Must be called from the UI thread
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            mPrompts = null;
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompts=");
            writer.println(mPrompts);
        }
    }
}