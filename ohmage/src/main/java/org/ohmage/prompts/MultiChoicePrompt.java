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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cketcham on 1/24/14.
 */
public class MultiChoicePrompt<T> extends ChoicePrompt<ArrayList<T>, T> {

    public Integer minChoices;

    public Integer maxChoices;

    public boolean isSkippable() {
        return skippable || (value != null && !((List) value).isEmpty() &&
                             (minChoices == null || ((List) value).size() >= minChoices) &&
                             (maxChoices == null || ((List) value).size() <= maxChoices));
    }

    @Override
    public SurveyItemFragment getFragment() {
        return MultiChoicePromptFragment.getInstance(this);
    }

    @Override
    public void addAnswer(JSONObject data, JSONObject extras) throws JSONException {
        if (value != null) {
            JSONArray array = new JSONArray();
            for (KLVPair object : (List<KLVPair>) value) {
                array.put(object.value);
            }
            data.put(surveyItemId, array);
        }
    }

    public List<T> getNewList() {
        return new ArrayList<T>();
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class MultiChoicePromptFragment<T>
            extends AnswerablePromptFragment<MultiChoicePrompt<T>> {

        public static MultiChoicePromptFragment getInstance(MultiChoicePrompt prompt) {
            MultiChoicePromptFragment fragment = new MultiChoicePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_choice, container, true);
            LinearLayout choiceContainer = (LinearLayout) view.findViewById(R.id.list);

            for (int i = 0; i < getPrompt().choices.size(); i++) {
                CheckedTextView v = (CheckedTextView) inflater
                        .inflate(android.R.layout.simple_list_item_multiple_choice, choiceContainer,
                                false);
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

                        T item = (T) getPrompt().choices.get(position);
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

                choiceContainer.addView(v);
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
        return value != null && value.contains(choices.get(position));
    }
}
