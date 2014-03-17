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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import org.ohmage.app.R;

/**
 * Created by cketcham on 1/23/14.
 */
public class TextPrompt extends AnswerablePrompt<String> {
    public int min;
    public int max;

    @Override
    public Fragment getFragment() {
        return TextPromptFragment.getInstance(this);
    }

    @Override
    public boolean isSkippable() {
        return skippable || !TextUtils.isEmpty(value);
    }

    public static class TextPromptFragment extends AnswerablePromptFragment<TextPrompt> {

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

        @Override
        public void onCreatePromptView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.prompt_text, container, true);

            EditText input = (EditText) rootView.findViewById(R.id.input);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    setValue(s.toString());
                }

                @Override public void afterTextChanged(Editable s) {
                }
            });
        }
    }
}
