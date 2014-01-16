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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ohmage.widget.VerticalViewPager;

/**
 * Created by cketcham on 12/18/13.
 */
public class SurveyActivity extends ActionBarActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (VerticalViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.gutter));

        mPagerAdapter = new PromptFragmentAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
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

    public class PromptFragmentAdapter extends FragmentStatePagerAdapter {
        public PromptFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new PromptItemFragment();
        }

        @Override
        public int getCount() {
            return 10;
        }

        @Override
        public float getPageWidth(int position) {
            if (position == getCount() - 1)
                return 1.0f;
            return 0.1f;
        }
    }

    public static class PromptItemFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate( R.layout.prompt_text, container, false);
        }
    }
}