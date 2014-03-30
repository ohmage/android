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

package org.ohmage.prompts;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.ohmage.app.R;
import org.ohmage.app.SurveyActivity;

/**
 * Created by cketcham on 1/23/14.
 */
public class TextPrompt extends AnswerablePrompt<String> {
    public int min;
    public int max;

    @Override
    public SurveyItemFragment getFragment() {
        return TextPromptFragment.getInstance(this);
    }

    @Override
    public boolean isSkippable() {
        return skippable || !TextUtils.isEmpty(value);
    }

    public boolean hasValidResponse() {
        return super.hasValidResponse() && !TextUtils.isEmpty(value);
    }

    public static class TextPromptFragment extends AnswerablePromptFragment<TextPrompt> {

        private static final String TAG = TextPromptFragment.class.getSimpleName();
        private EditText mInput;

        public static TextPromptFragment getInstance(TextPrompt prompt) {
            TextPromptFragment fragment = new TextPromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        protected boolean skippableStateChanged(Object o1, Object n1) {
            //TODO: remove this casting hack...
            String o = (String) o1;
            String n = (String) n1;
            return (TextUtils.isEmpty(o) && !TextUtils.isEmpty(n))
                   || (!TextUtils.isEmpty(o) && TextUtils.isEmpty(n));
        }

        @Override protected void onOkPressed() {
            hideKeyboard();
            super.onOkPressed();
        }

        @Override protected void onSkipPressed() {
            hideKeyboard();
            super.onSkipPressed();
        }

        public boolean nextPromptIsTextPrompt() {
            SurveyActivity.PromptFragmentAdapter adapter =
                    ((SurveyActivity) getActivity()).mPager.getAdapter();
            int position = adapter.getPromptPosition(getPrompt());
            if (position + 1 < adapter.getPromptCount()) {
                return adapter.getPromptAt(position + 1) instanceof TextPrompt &&
                       adapter.getObject(position + 1).mHidden;
            }
            return false;
        }

        public void hideKeyboard() {
            if (!nextPromptIsTextPrompt()) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
            }
        }

        @Override protected void setHidden(View view, boolean hidden, int duration) {
            super.setHidden(view, hidden, duration);
            if (!hidden && mInput != null && getActivity() != null) {
                Configuration config = getResources().getConfiguration();
                if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
                    // It has to request focus after the prompt has moved up, otherwise it will
                    // scroll too far. I may have to fix this so it always happens after being moved
                    // rather than after 200ms
                    mInput.postDelayed(new Runnable() {
                        @Override public void run() {
                            mInput.requestFocus();
                        }
                    }, 200);

                    mInput.post(new Runnable() {
                        @Override public void run() {
                            if(getActivity() != null) {
                                InputMethodManager inputMgr =
                                        (InputMethodManager) getActivity().getSystemService(
                                                Context.INPUT_METHOD_SERVICE);
                                inputMgr.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.prompt_text, container, true);

            mInput = (EditText) rootView.findViewById(R.id.input);
            mInput.requestFocus();
            mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.id.submit_prompt || id == EditorInfo.IME_NULL) {
                        dispatchOkPressed();
                        return true;
                    }
                    return false;
                }
            });
            mInput.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Catch the case where the fragment saved state tries to set this text as empty
                    if (count > 0 || before > 0) {
                        setValue(s.toString());
                    }
                }

                @Override public void afterTextChanged(Editable s) {
                }
            });
        }
    }
}
