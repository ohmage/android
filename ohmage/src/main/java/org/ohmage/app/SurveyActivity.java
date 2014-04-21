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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.viewpagerindicator.CirclePageIndicator;

import org.ohmage.condition.Condition;
import org.ohmage.condition.NoResponse;
import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.fragments.InstallDependenciesDialog;
import org.ohmage.models.ApkSet;
import org.ohmage.prompts.AnswerablePrompt;
import org.ohmage.prompts.Prompt;
import org.ohmage.prompts.PromptFragment;
import org.ohmage.prompts.SurveyItemFragment;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.provider.ResponseContract.Responses;
import org.ohmage.reminders.glue.TriggerFramework;
import org.ohmage.streams.StreamPointBuilder;
import org.ohmage.widget.VerticalViewPager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by cketcham on 12/18/13.
 */
public class SurveyActivity extends InjectedActionBarActivity
        implements LoaderCallbacks<ArrayList<Prompt>>, ConnectionCallbacks,
        OnConnectionFailedListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private VerticalViewPager mPager;

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

    private SurveyStateFragment mState;

    public static final int MSG_SHOW_INSTALL_DEPENDENCIES = 0;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_SHOW_INSTALL_DEPENDENCIES) {
                FragmentManager fm = getSupportFragmentManager();
                InstallDependenciesDialog fragment =
                        (InstallDependenciesDialog) fm.findFragmentByTag("install");
                if (fragment == null) {
                    fragment = InstallDependenciesDialog.getInstance((ApkSet) msg.obj, true);
                }
                fragment.show(fm, "install");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (VerticalViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.gutter));

        //Bind the title indicator to the adapter
        indicator = (CirclePageIndicator) findViewById(R.id.titles);

        if (savedInstanceState == null) {
            mState = new SurveyStateFragment();
            getSupportFragmentManager().beginTransaction().add(mState, "state").commit();
        } else {
            mState = (SurveyStateFragment) getSupportFragmentManager().findFragmentByTag("state");
        }

        if (mState.prompts != null) {
            setPromptFragmentAdapter();
        } else {
            getSupportLoaderManager().initLoader(0, null, this);
        }
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

            // If nothing has been answered just go back
            if (mPagerAdapter.getAnsweredCount() == 0) {
                discardSurvey();
                return;
            }

            // Otherwise show a dialog so they don't lose their responses
            FragmentManager fm = getSupportFragmentManager();
            CancelResponseDialogFragment fragment =
                    (CancelResponseDialogFragment) fm.findFragmentByTag("cancel");
            if (fragment == null) {
                fragment = new CancelResponseDialogFragment();
            }
            fragment.show(fm, "cancel");
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    private void discardSurvey() {
        mPagerAdapter.clearExtras();
        super.onBackPressed();
    }

    @Override public SurveyPromptLoader onCreateLoader(int id, Bundle args) {
        return new SurveyPromptLoader(this, getIntent().getData());
    }

    @Override public void onLoadFinished(Loader<ArrayList<Prompt>> loader, ArrayList<Prompt> data) {
        if (mPagerAdapter == null) {
            setPrompts(data);

            // Check for remote activity prompts and make sure they all exist
            ApkSet appItems = ApkSet.fromPromptsIgnoreSkippable(data);
            appItems.clearInstalled(this);

            if(!appItems.isEmpty()) {
                Message msg = handler.obtainMessage(MSG_SHOW_INSTALL_DEPENDENCIES);
                msg.obj = appItems;
                msg.sendToTarget();
            }
        }
    }

    @Override public void onLoaderReset(Loader<ArrayList<Prompt>> loader) {

    }

    public void setPromptFragmentAdapter() {
        mPagerAdapter = new PromptFragmentAdapter(getSupportFragmentManager(), mState.prompts);
        mPager.setAdapter(mPagerAdapter);
        indicator.setViewPager(mPager);
    }

    public void setPrompts(ArrayList<Prompt> data) {
        mState.prompts = new Prompts(data);
        setPromptFragmentAdapter();
    }

    public void submit() {
        // Tell reminders that the survey was taken
        TriggerFramework.notifySurveyTaken(this, Surveys.getId(getIntent().getData()));

        ContentValues values = new ContentValues();
        values.put(Responses.SURVEY_ID, Surveys.getId(getIntent().getData()));
        values.put(Responses.SURVEY_VERSION, Surveys.getVersion(getIntent().getData()));
        values.put(Responses.RESPONSE_METADATA,
                new StreamPointBuilder().now().withId()
                        .withLocation(mLocationClient.getLastLocation())
                        .getMetadata()
        );
        mPagerAdapter.buildResponse(values);
        getContentResolver().insert(Responses.CONTENT_URI, values);
        finish();
    }

    @Override public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(REQUEST, new LocationListener() {
            @Override public void onLocationChanged(Location location) {
            }
        });
    }

    @Override public void onDisconnected() {

    }

    @Override public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public PromptFragmentAdapter getPagerAdapter() {
        return mPagerAdapter;
    }

    public class PromptFragmentAdapter extends FragmentStatePagerAdapter {
        private final Prompts prompts;

        private final FragmentManager mFragmentManager;

        Gson gson = new GsonBuilder().create();

        /**
         * Keeps track of what fragments have been created
         */
        private BitSet mFragments = new BitSet();

        /**
         * Keeps track of any prompts which have just changed due to an answer being updated.
         */
        int mLastUpdate = 0;

        public PromptFragmentAdapter(FragmentManager fm, Prompts prompts) {
            super(fm);
            mFragmentManager = fm;
            this.prompts = prompts;
        }

        @Override
        public SurveyItemFragment getItem(final int position) {
            Prompt prompt = prompts.getPromptAt(position);
            SurveyItemFragment fragment = null;
            if (prompt == null) {
                fragment = new SubmitResponseFragment();
            } else {
                fragment = prompt.getFragment();
                fragment.showButtons(position == getCount() - 1 ? View.VISIBLE : View.GONE);
            }

            if (fragment != null) {
                mFragments.set(position);
            }

            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.clear(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return prompts.getAnsweredCount() + 1;
        }

        @Override
        public float getPageWidth(int position) {
            if (position == prompts.size()) {
                return 1.0f;
            }
            return 0.1f;
        }

        @Override public Parcelable saveState() {
            Parcelable parent = super.saveState();
            Bundle state = new Bundle();
            state.putParcelable("parent", parent);
            return state;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            Bundle bundle = (Bundle) state;
            state = bundle.getBundle("parent");

            // Hijack the fragment state from our parent
            if (state != null) {
                bundle = (Bundle) state;
                mFragments.clear();
                Iterable<String> keys = bundle.keySet();
                for (String key : keys) {
                    if (key.startsWith("f")) {
                        int index = Integer.parseInt(key.substring(1));
                        Fragment f = mFragmentManager.getFragment(bundle, key);
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

        public void buildResponse(ContentValues values) {
            Log.d(TAG, gson.toJson(prompts.answers).toString());
            values.put(Responses.RESPONSE_DATA, gson.toJson(prompts.answers));
            values.put(Responses.RESPONSE_EXTRAS, gson.toJson(prompts.extras));
        }

        public void updateAnswer(PromptFragment promptFragment) {
            promptFragment.showButtons(View.GONE);
            Prompt prompt = promptFragment.getPrompt();
            boolean alreadyAnswered = prompts.isAnswered(prompt);

            mLastUpdate = prompts.updateAnswer(prompt);
            notifyDataSetChanged();

            if (!alreadyAnswered) {
                moveToLastPrompt();
            }
        }

        /**
         * Moves to the last prompt, waits until it has been added to the view
         */
        private void moveToLastPrompt() {
            final int position = getCount() - 1;
            mPager.post(new Runnable() {
                @Override public void run() {
                    mPager.bringPositionUpOnScreen(position);
                }
            });
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof PromptFragment) {
                if (prompts.positionOf(((PromptFragment) object).getPrompt()) >= mLastUpdate) {
                    return PagerAdapter.POSITION_NONE;
                }
                return POSITION_UNCHANGED;
            }
            return PagerAdapter.POSITION_NONE;
        }

        public int getPosition(SurveyItemFragment fragment) {
            if (fragment instanceof PromptFragment) {
                return prompts.positionOf(((PromptFragment) fragment).getPrompt());
            } else if (fragment instanceof SubmitResponseFragment) {
                return prompts.size();
            }
            return -1;
        }

        public SurveyItemFragment getPromptFragmentAt(int position) {
            if (mFragments.get(position)) {
                return getItem(position);
            }
            return null;
        }

        public int getAnsweredCount() {
            return prompts.getAnsweredCount();
        }

        public void clearExtras() {
            prompts.clearExtras();
        }
    }

    public static class SubmitResponseFragment extends SurveyItemFragment {

        boolean submitted = false;

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.prompt_submit, container, false);
            final Button submit = (Button) view.findViewById(R.id.submit);
            submit.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    submit.setEnabled(false);
                    if (!submitted) {
                        submitted = true;
                        ((SurveyActivity) getActivity()).submit();
                    }
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

    public static class SurveyStateFragment extends Fragment {
        public Prompts prompts;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

    /**
     * This class keeps track of all the prompts and how the user answered them. It provides the
     * functions {@link #answer(AnswerablePrompt)}, {@link #ignore(Prompt)}, and
     * {@link #skip(Prompt)} based on the users actions. {@link #getPromptAt(int)} will return the
     * next prompt ignoring all prompts which weren't displayed.
     */
    public static class Prompts {
        final ArrayList<Prompt> prompts;

        // Answers
        final public HashMap<String, Object> answers;
        final public HashMap<String, String> extras;

        // List of the skipped prompts
        final ArrayList<String> mSkipped;

        // List of not displayed prompts
        final ArrayList<String> mNotDisplayed;

        public Prompts(ArrayList<Prompt> data) {
            this.prompts = data;

            answers = new HashMap<String, Object>();
            extras = new HashMap<String, String>();
            mSkipped = new ArrayList<String>();
            mNotDisplayed = new ArrayList<String>();
        }

        /**
         * Returns the prompt at the given position ignoring prompts which are not displayed
         *
         * @param position
         * @return
         */
        public Prompt getPromptAt(int position) throws IllegalStateException {
            Prompt prompt = null;
            Map<String, Object> prev = null;
            int index = promptIndex(position);

            while (prompt == null && index < prompts.size()) {
                prompt = prompts.get(index);
                if (prompt.getCondition() != null) {
                    if (prev == null) {
                        prev = getPreviousResponses(index);
                    }
                    Condition condition = new Condition(prompt.getCondition());

                    if (!condition.evaluate(prev)) {
                        ignore(prompt);
                        updatePrevious(prompt, prev);
                        prompt = null;
                        index++;
                    }
                }
            }
            return prompt;
        }

        /**
         * Returns the index into {@link #prompts} given the position of shown prompts
         *
         * @param position
         * @return
         */
        private int promptIndex(int position) {
            return position + positionOffset(position);
        }

        private int positionOffset(int index) {
            int count = 0;
            for (int i = 0; i <= index && i < prompts.size(); i++) {
                if (isIgnored(prompts.get(i))) {
                    count++;
                    index++;
                }
            }
            return count;
        }

        /**
         * Count the number of prompts that have been ignored before the given position
         *
         * @param index
         * @return
         */
        private int ignoredPromptsBefore(int index) {
            int count = 0;
            for (int i = 0; i < index && i < prompts.size(); i++) {
                if (isIgnored(prompts.get(i))) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Gets the map of all answers before the current answer to be used to evaluate the
         * the condition for the prompt at the given position
         *
         * @return a map of all previous responses
         */
        private Map<String, Object> getPreviousResponses(int index) {
            AbstractMap prev = new HashMap<String, Object>();
            for (int i = 0; i < index; i++) {
                updatePrevious(prompts.get(i), prev);
            }
            return prev;
        }

        /**
         * Convenience method to update the previous responses map for the given prompt
         *
         * @param prompt
         * @param prev
         */
        private void updatePrevious(Prompt prompt, Map<String, Object> prev) {
            if (answers.keySet().contains(prompt.getId())) {
                prev.put(prompt.getId(), answers.get(prompt.getId()));
            } else if (mSkipped.contains(prompt.getId())) {
                prev.put(prompt.getId(), NoResponse.SKIPPED);
            } else if (mNotDisplayed.contains(prompt.getId())) {
                prev.put(prompt.getId(), NoResponse.NOT_DISPLAYED);
            }
        }

        /**
         * Calculates the number of prompts that were actively answered or skipped.
         *
         * @return
         */
        public int getAnsweredCount() {
            return answers.size() + mSkipped.size();
        }

        /**
         * Will add an answer given the state of the prompt item
         *
         * @param prompt
         * @return the index of the first position which changed due to conditions
         */
        public int updateAnswer(Prompt prompt) {
            if (prompt instanceof AnswerablePrompt) {
                if (((AnswerablePrompt) prompt).hasValidResponse()) {
                    answer((AnswerablePrompt) prompt);
                } else if (prompt.isSkippable()) {
                    skip(prompt);
                }
            } else {
                answer(prompt);
            }

            return resetFutureAnswers(prompt);
        }

        private void answer(Prompt prompt) {
            remove(prompt);
            answers.put(prompt.getId(), null);
        }

        private void answer(AnswerablePrompt prompt) {
            remove(prompt);
            answers.put(prompt.getId(), prompt.getAnswer());
            extras.put(prompt.getId(), prompt.getAnswerExtras());
        }

        private void ignore(Prompt prompt) {
            remove(prompt);
            mNotDisplayed.add(prompt.getId());
        }

        private void skip(Prompt prompt) {
            remove(prompt);
            mSkipped.add(prompt.getId());
        }

        private void remove(Prompt prompt) {
            mNotDisplayed.remove(prompt.getId());
            mSkipped.remove(prompt.getId());
            answers.remove(prompt.getId());
            extras.remove(prompt.getId());
        }

        /**
         * Calculates if a prompt was answered
         *
         * @param prompt
         * @return true if it was answered or skipped
         */
        public boolean isAnswered(Prompt prompt) {
            return answers.containsKey(prompt.getId()) || mSkipped.contains(prompt.getId());
        }

        /**
         * Calculates if a prompt was ignored due to conditions
         *
         * @param prompt
         * @return true if it was ignored
         */
        public boolean isIgnored(Prompt prompt) {
            return mNotDisplayed.contains(prompt.getId());
        }

        public int ignoredPromptsSize() {
            return mNotDisplayed.size();
        }

        public int size() {
            return prompts.size() - mNotDisplayed.size();
        }

        public int positionOf(Prompt prompt) {
            int idx = prompts.indexOf(prompt);
            return idx - ignoredPromptsBefore(idx);
        }

        private void removeAnswersAfter(int index) {
            for (int i = index; i < prompts.size(); i++) {
                Prompt prompt = prompts.get(i);
                String promptId = prompt.getId();
                if (mSkipped.contains(promptId)) {
                    mSkipped.remove(promptId);
                } else if (answers.keySet().contains(promptId)) {
                    answers.remove(promptId);
                    if(extras.keySet().contains(promptId)) {
                        extras.remove(promptId);
                    }
                } else if (mNotDisplayed.contains(promptId)) {
                    mNotDisplayed.remove(promptId);
                }
            }
        }

        public void clearExtras() {
            for (int i = 0; i < prompts.size(); i++) {
                String fileName = extras.get(prompts.get(i).getId());
                if (fileName != null) {
                    new File(fileName).delete();
                }
            }
        }

        /**
         * Since the answer changed for this response we should re-calculate all of the next responses
         *
         * @param prompt
         */
        private int resetFutureAnswers(Prompt prompt) throws IllegalStateException {
            int pivot = prompts.indexOf(prompt);
            Map<String, Object> prev = null;

            int lastUpdate = size();

            // Calculate the first prompt which no longer passes the condition test
            int after = prompts.size();
            for (int i = pivot + 1;
                 i < getAnsweredCount() + ignoredPromptsSize() && i < prompts.size(); i++) {
                prompt = prompts.get(i);
                if (prev == null) {
                    prev = getPreviousResponses(i);
                }
                if (prompt.getCondition() != null) {
                    Condition condition = new Condition(prompt.getCondition());
                    // The first prompt which is not the same as it used to be and is shown
                    boolean show = condition.evaluate(prev);
                    if (show == isIgnored(prompt)) {
                        lastUpdate = Math.min(lastUpdate, i);
                        if (!show) {
                            ignore(prompt);
                        } else {
                            after = i;
                            break;
                        }
                    }
                }

                updatePrevious(prompt, prev);
            }

            // Remove all responses after the last valid one
            removeAnswersAfter(after);

            return lastUpdate - ignoredPromptsBefore(lastUpdate + 1);
        }
    }

    public static class CancelResponseDialogFragment extends DialogFragment {

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.discard_survey_title))
                    .setMessage(getString(R.string.discard_survey_message))
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            ((SurveyActivity) getActivity()).discardSurvey();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }
}