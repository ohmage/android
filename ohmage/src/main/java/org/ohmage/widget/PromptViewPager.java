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
import android.view.View;

import org.ohmage.app.SurveyActivity;
import org.ohmage.app.SurveyActivity.BasePromptAdapterFragment;
import org.ohmage.prompts.AnswerablePrompt;
import org.ohmage.prompts.BasePrompt.AnswerablePromptFragment;
import org.ohmage.prompts.BasePrompt.AnswerablePromptFragment.OnValidAnswerStateChangedListener;

/**
 * Created by cketcham on 1/15/14.
 */
public class PromptViewPager extends VerticalViewPager implements
        OnValidAnswerStateChangedListener {
    private static final String TAG = PromptViewPager.class.getSimpleName();

    private double mSoftBottomBound = Double.MAX_VALUE;

    private int mLastValidPromptItem = 0;

    public PromptViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (adapter != null && !(adapter instanceof SurveyActivity.PromptFragmentAdapter))
            throw new RuntimeException("PromptViewPager expects a PromptFragmentAdapter");
        super.setAdapter(adapter);
    }

    @Override
    public SurveyActivity.PromptFragmentAdapter getAdapter() {
        return (SurveyActivity.PromptFragmentAdapter) super.getAdapter();
    }

    @Override
    public void scrollTo(int x, int y) {
        y = (int) Math.min(y, mSoftBottomBound);
        super.scrollTo(x, y);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getAdapter() != null) {
            updateLastValidResponse(0);
            calculateBottomBound();
        }
    }

    private void updateLastValidResponse(int index) {
        final int N = getAdapter().getCount();
        for (int i = Math.min(mLastValidPromptItem, index); i < N; i++) {
            BasePromptAdapterFragment item = (BasePromptAdapterFragment) getAdapter().getObject(i);
            boolean stopHere = false;
            if (!item.getOkPressed()) {
                stopHere = true;
            } else if (item instanceof AnswerablePromptFragment) {
                AnswerablePromptFragment fragment = (AnswerablePromptFragment) item;
                AnswerablePrompt prompt = (AnswerablePrompt) fragment.getPrompt();
                if (!prompt.hasValidResponse() || !fragment.getOkPressed()) {
                    break;
                }
            }

            if(stopHere) {
            item.setHidden(false);
            // Show the buttons for this prompt
            item.showButtons(View.VISIBLE);
            // Hide any prompts which may be after this prompt
            hideAllPromptsBetween(i, mLastValidPromptItem);
                if(mLastValidPromptItem != i)
                    setCurrentItem(i);
            mLastValidPromptItem = i;
                return;
        }


                item.setHidden(false);
                item.showButtons(View.GONE);
    }
    }

    private void hideAllPromptsBetween(int start, int end) {
        for (int i = start+1; i <= end; i++) {
            Fragment item = getAdapter().getObject(i);
            if (item instanceof AnswerablePromptFragment) {
                ((AnswerablePromptFragment) item).setHidden(true);
            }
        }
    }

    private void calculateBottomBound() {
        ItemInfo ii = infoForPosition(mLastValidPromptItem);
        mSoftBottomBound = ii.offset * getHeight();
        return;
    }

    @Override public void onValidAnswerStateChanged(AnswerablePrompt prompt) {
        int index = getAdapter().getPromptPosition(prompt);
        updateLastValidResponse(index);
        calculateBottomBound();
    }

    public void goToNext() {
        updateLastValidResponse(mLastValidPromptItem+1);
        calculateBottomBound();
    }
}