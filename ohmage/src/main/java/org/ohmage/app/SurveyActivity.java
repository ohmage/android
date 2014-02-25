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

import android.content.Context;
import android.database.Cursor;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viewpagerindicator.CirclePageIndicator;

import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.prompts.AnswerablePrompt;
import org.ohmage.prompts.BasePrompt;
import org.ohmage.prompts.BasePrompt.AnswerablePromptFragment;
import org.ohmage.prompts.BasePrompt.AnswerablePromptFragment.OnSkipStateChanged;
import org.ohmage.prompts.Prompt;
import org.ohmage.provider.OhmageContract;
import org.ohmage.widget.CompatArrayAdapter;
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
        implements LoaderCallbacks<ArrayList<Prompt>> {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    @Inject
    OhmageService ohmageService;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private PromptViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PromptFragmentAdapter mPagerAdapter;

    private CirclePageIndicator indicator;

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
        mPagerAdapter.buildResponse();
        }

    public class PromptFragmentAdapter extends FragmentStatePagerAdapter implements
            OnSkipStateChanged {
        private final List<Prompt> mPrompts;

        int mSelected = 0;

        /**
         * Keeps track of what fragments have been created
         */
        private BitSet mFragments = new BitSet();

        public PromptFragmentAdapter(FragmentManager fm, List<Prompt> prompts) {
            super(fm);
            mPrompts = prompts;
            mSelected = mPager.getCurrentItem();
        }

        @Override
        public Fragment getItem(final int position) {
            Fragment fragment = null;
            if (position == getCount() - 1) {
                fragment = new SubmitResponseFragment();
            } else {
                fragment = mPrompts.get(position).getFragment();
                if (fragment instanceof AnswerablePromptFragment) {
                    ((AnswerablePromptFragment) fragment).setOnSkipStateChangedListener(this);
                }
            }

            if (mSelected == position) {
                ((BasePromptAdapterFragment) fragment).setEnabled(true);
            }
            ((BasePromptAdapterFragment) fragment).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean canJumpAhead = true;
                    for (int i = mPager.getCurrentItem(); i < position; i++) {
                        canJumpAhead &= canPassItem(i);
                    }
                    if (canJumpAhead)
                        mPager.setCurrentItem(position, true);
                }
            });
            mFragments.set(position);

            return fragment;
        }

        public boolean canPassItem(int position) {
            Fragment selectedFragment = getObject(position);
            if (selectedFragment instanceof BasePrompt.PromptFragment) {
                return ((BasePrompt.PromptFragment) selectedFragment).getPrompt().isSkippable();
            }
            return true;
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

        public void setSelected(int position) {
            // Only change the selected item if it has changed
            if (mSelected == position)
                return;

            // Check to see that the fragments exist
            if (!mFragments.get(mSelected) || !mFragments.get(position)) {
                return;
            }

            Object item = super.instantiateItem(null, mSelected);
            if (item instanceof BasePromptAdapterFragment) {
                ((BasePromptAdapterFragment) item).setEnabled(false);
            }
            mSelected = position;
            item = super.instantiateItem(null, mSelected);
            if (item instanceof BasePromptAdapterFragment) {
                ((BasePromptAdapterFragment) item).setEnabled(true);
            }
        }

        @Override
        public Parcelable saveState() {
            Bundle bundle = new Bundle();
            bundle.putSerializable("fragments", mFragments);
            bundle.putParcelable("superstate", super.saveState());
            return bundle;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            if (state != null) {
                Bundle bundle = (Bundle) state;
                mFragments = (BitSet) bundle.getSerializable("fragments");
                super.restoreState(bundle.getParcelable("superstate"), loader);
            }
        }

        @Override public void onSkipStateChanged(AnswerablePrompt prompt) {
            mPager.onValueChanged(prompt);
        }

        public void buildResponse() {
            for (int i = 0; i < getCount(); i++) {
                Fragment item = getItem(i);
                if (item instanceof AnswerablePromptFragment) {
                    //TODO: remove some casting?
                    AnswerablePrompt prompt =
                            (AnswerablePrompt) ((AnswerablePromptFragment) item).getPrompt();
                    Log.d(TAG, prompt.getId() + "=" + prompt.value);
                }
            }
        }
    }

    public static class PromptAdapter extends CompatArrayAdapter<Prompt> {

        public PromptAdapter(Context context) {
            super(context, 0);
        }
    }

    public static class BasePromptAdapterFragment extends Fragment {

        boolean mEnabled = false;

        boolean mSkipped = false;

        private View.OnClickListener mOnClickListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            if (savedInstanceState != null) {
                mEnabled = savedInstanceState.getBoolean("enabled", false);
                mSkipped = savedInstanceState.getBoolean("skipped", false);
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            view.setOnClickListener(mOnClickListener);
            setEnabled((ViewGroup) view, mEnabled);
            setSkipped(view, mSkipped, 0);
        }

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("enabled", mEnabled);
            outState.putBoolean("skipped", mSkipped);
        }

        public void setEnabled(boolean enabled) {
            setEnabled((ViewGroup) getView(), enabled);
        }

        protected void setEnabled(ViewGroup view, boolean enabled) {
            mEnabled = enabled;
//            if (view == null)
//                return;
//            ViewGroup container = (ViewGroup) view.getChildAt(0);
//            for(int i=0;i<container.getChildCount();i++) {
//                View child = container.getChildAt(i);
//                child.setEnabled(enabled);
//                child.setClickable(enabled);
//                child.setLongClickable(enabled);
//                child.setFocusable(enabled);
//            }
        }

        public void setSkipped(boolean skipped) {
            if (mSkipped == skipped)
                return;
            setSkipped(getView(), skipped, 200);
        }

        public void setSkipped(boolean skipped, int duration) {
            if (mSkipped == skipped)
                return;
            setSkipped(getView(), skipped, duration);
        }

        protected void setSkipped(View view, boolean skipped, int duration) {
            mSkipped = skipped;
            if (view == null)
                return;
            AlphaAnimation aa = (skipped) ? new AlphaAnimation(1f, 0f) : new AlphaAnimation(0f, 1f);
            aa.setFillAfter(true);
            aa.setDuration(duration);
            view.startAnimation(aa);
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
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