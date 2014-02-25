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

package org.ohmage.widget;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.ohmage.app.SurveyActivity;
import org.ohmage.prompts.AnswerablePrompt;

/**
 * Created by cketcham on 1/15/14.
 */
public class PromptViewPager extends VerticalViewPager {
    private static final String TAG = PromptViewPager.class.getSimpleName();

    private double mSoftBottomBound = Double.MAX_VALUE;

    public PromptViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean ret = super.onTouchEvent(ev);
        scrollBack(ev);
        return ret;
    }

    public void setAdapter(SurveyActivity.PromptFragmentAdapter adapter) {
        super.setAdapter(adapter);

        updateSkipped();
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (adapter != null && !(adapter instanceof SurveyActivity.PromptFragmentAdapter))
            throw new RuntimeException("PromptViewPager expects a PromptFragmentAdapter");
        super.setAdapter(adapter);

        updateSkipped();
    }

    @Override
    public SurveyActivity.PromptFragmentAdapter getAdapter() {
        return (SurveyActivity.PromptFragmentAdapter) super.getAdapter();
    }

    //TODO: don't scroll back to the top of the item if the bottom of the item is still on the screen ie. when there is a long prompt
    private void scrollBack(MotionEvent ev) {
//        final int action = ev.getAction();
//
//        switch (action & MotionEventCompat.ACTION_MASK) {
//            case MotionEvent.ACTION_UP:
//                for (int i = 0; i < getCurrentItem(); i++) {
//                    if(!getAdapter().canPassItem(i)) {
//                        setCurrentItem(i, true);
//                        break;
//                    }
//                }
//                break;
//        }
    }

    @Override
    public void scrollTo(int x, int y) {
        y = (int) Math.min(y, mSoftBottomBound);
        super.scrollTo(x, y);
    }

    @Override
    protected void onPageScrolled(int position, double offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);

        if (offsetPixels < mSoftBottomBound) {
            getAdapter().setSelected(position);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getAdapter() != null)
            calculateBottomBound();
    }

    public void onValueChanged(AnswerablePrompt prompt) {
        calculateBottomBound();
        updateSkipped();
    }

    private void calculateBottomBound() {
        final int N = getAdapter().getCount();
        for (int i = 0; i < N; i++) {
            if (!getAdapter().canPassItem(i)) {
                ItemInfo ii = infoForPosition(i);
                mSoftBottomBound =
                        (ii.offset + ii.heightFactor) * getHeight() + getPageMargin() - 1;
                return;
            }
        }
        mSoftBottomBound = Double.MAX_VALUE;
        return;
    }

    public void finishUpdate() {
        calculateBottomBound();
    }

    //TODO: something should keep track of the pivot position so we don't need to keep calculating it
    private void updateSkipped() {
        boolean skippedPivot = false;
        final int N = getAdapter().getCount();
        for (int i = Math.max(getCurrentItem() - 1, 0); i < N; i++) {
            Fragment item = getAdapter().getObject(i);
            if (item instanceof SurveyActivity.BasePromptAdapterFragment) {
                ((SurveyActivity.BasePromptAdapterFragment) item).setSkipped(skippedPivot);
            }
            skippedPivot |= !getAdapter().canPassItem(i);
        }

        // Move the current item back to the top of the last item if this item is wrong
        if (getCurrentItem() > 0 && !getAdapter().canPassItem(getCurrentItem() - 1)) {
            post(new Runnable() {
                @Override public void run() {
                    setCurrentItem(getCurrentItem() - 1, true);
                }
            });
        }
    }
}