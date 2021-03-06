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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import org.ohmage.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cketcham on 1/24/14.
 */
public class MultiChoicePrompt<T> extends ChoicePrompt<ArrayList<T>, T> {

    public Integer minChoices;

    public Integer maxChoices;

    @Override public boolean hasValidResponse() {
        return super.hasValidResponse() && !((List) value).isEmpty() &&
               (minChoices == null || ((List) value).size() >= minChoices) &&
               (maxChoices == null || ((List) value).size() <= maxChoices);
    }

    @Override
    public SurveyItemFragment getFragment() {
        return MultiChoicePromptFragment.getInstance(this);
    }

    public List<T> getNewList() {
        return new ArrayList<T>();
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class MultiChoicePromptFragment<T>
            extends AnswerablePromptFragment<MultiChoicePrompt<T>> {

        private LinearLayout mChoiceContainer;

        public static MultiChoicePromptFragment getInstance(MultiChoicePrompt prompt) {
            MultiChoicePromptFragment fragment = new MultiChoicePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_choice, container, true);
            mChoiceContainer = (LinearLayout) view.findViewById(R.id.list);

            for (int i = 0; i < getPrompt().choices.size(); i++) {
                CheckedTextView v = (CheckedTextView) inflater
                        .inflate(android.R.layout.simple_list_item_multiple_choice,
                                mChoiceContainer, false);
                v.setText(getPrompt().choices.get(i));

                if (getPrompt().positionIsChecked(i)) {
                    v.setChecked(true);
                }

                final int position = i;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        List answer = getPrompt().getNewList();
                        if (getPrompt().value != null) {
                            answer.addAll((List) getPrompt().value);
                        }

                        T item = getPrompt().choices.get(position).value;
                        if (!answer.contains(item)) {
                            answer.add(item);
                            ((CheckedTextView) v).setChecked(true);
                        } else {
                            answer.remove(item);
                            ((CheckedTextView) v).setChecked(false);
                        }
                        setValue(answer);
                    }
                });

                mChoiceContainer.addView(v);
            }
        }

        @Override protected void onSkipPressed() {
            super.onSkipPressed();
            for (int i = 0; i < mChoiceContainer.getChildCount(); i++) {
                ((CheckedTextView) mChoiceContainer.getChildAt(i)).setChecked(false);
            }
        }
    }

    private boolean[] checkedItems() {
        boolean[] checkedItems = new boolean[choices.size()];
        if (value != null) {
            for (int i = 0; i < value.size(); i++) {
                checkedItems[choices.indexOfValue(value.get(i))] = true;
            }
        }
        return checkedItems;
    }

    private boolean positionIsChecked(int position) {
        return value != null && value.contains(choices.get(position).value);
    }
}
