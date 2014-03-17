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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import org.ohmage.app.R;

/**
 * Created by cketcham on 1/24/14.
 */
public class SingleChoicePrompt<T> extends ChoicePrompt<T, T> {

    private int getCheckedItem() {
        return (value != null) ? choices.indexOfValue(value) : -1;
    }

    @Override
    public Fragment getFragment() {
        return SingleChoicePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class SingleChoicePromptFragment<T>
            extends AnswerablePromptFragment<SingleChoicePrompt<T>> {

        public static SingleChoicePromptFragment getInstance(SingleChoicePrompt prompt) {
            SingleChoicePromptFragment fragment = new SingleChoicePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_choice, container, true);
            final LinearLayout choiceContainer = (LinearLayout) view.findViewById(R.id.list);

            for(int i=0;i<getPrompt().choices.size();i++) {
                CheckedTextView v = (CheckedTextView) inflater.inflate(android.R.layout.simple_list_item_single_choice, choiceContainer, false);
                v.setText(getPrompt().choices.get(i));

                if(getPrompt().getCheckedItem() == i) {
                    v.setChecked(true);
                }

                final int position = i;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if(getPrompt().value != null) {
                            View previous = choiceContainer.getChildAt(
                                    getPrompt().choices.indexOfValue(getPrompt().value));
                            if(previous instanceof CheckedTextView) {
                                ((CheckedTextView) previous).setChecked(false);
                            }
                        }

                        Object selected = getPrompt().choices.get(position).value;
                        ((CheckedTextView)v).setChecked(!selected.equals(getPrompt().value));
                        setValue((selected.equals(getPrompt().value)) ? null : selected);
                    }
                });
                choiceContainer.addView(v);
            }
        }
    }
}
