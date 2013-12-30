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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cketcham on 1/24/14.
 */
public class MultiChoicePrompt<T> extends ChoicePrompt<ArrayList<T>> {

    public Integer minChoices;

    public Integer maxChoices;

    public boolean isSkippable() {
        return skippable || (value != null && !((List) value).isEmpty() &&
                             (minChoices == null || ((List) value).size() >= minChoices) &&
                             (maxChoices == null || ((List) value).size() <= maxChoices));
    }

    @Override
    public Fragment getFragment() {
        return MultiChoicePromptFragment.getInstance(this);
    }

    @Override
    public void addAnswer(JSONObject data, JSONObject extras) throws JSONException {
        if(value != null) {
            JSONArray array = new JSONArray();
            for(T object : (List<T>)value) {
                array.put(object);
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
            extends PromptLauncherFragment<MultiChoicePrompt<T>>
            implements OnMultiChoiceClickListener {

        public static MultiChoicePromptFragment getInstance(MultiChoicePrompt prompt) {
            MultiChoicePromptFragment fragment = new MultiChoicePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override protected String getLaunchButtonText() {
            return getString(R.string.show_choices);
        }

        @Override public void onClick(View v) {
            MultiChoiceDialogFragment.getInstance(this, getPrompt().choices)
                    .show(getFragmentManager(), "dialog");
        }

        @Override public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            List answer = getPrompt().getNewList();
            if (getPrompt().value != null)
                answer.addAll((List) getPrompt().value);
            if (isChecked) {
                answer.add(getPrompt().choices.get(which).value);
            } else {
                answer.remove(getPrompt().choices.get(which).value);
            }
            setValue(answer);
        }
    }

    public static class MultiChoiceDialogFragment extends DialogFragment {
        private OnMultiChoiceClickListener mListener;

        public static <T> DialogFragment getInstance(OnMultiChoiceClickListener l,
                ArrayList<KLVPair<T>> choices) {
            MultiChoiceDialogFragment fragment = new MultiChoiceDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("choices", choices);
            fragment.setArguments(args);
            fragment.setOnMultiChoiceClickListener(l);
            return fragment;
        }

        public void setOnMultiChoiceClickListener(OnMultiChoiceClickListener listener) {
            mListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            ArrayList<KLVPair> choices =
                    (ArrayList<KLVPair>) getArguments().getSerializable("choices");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMultiChoiceItems(choices.toArray(new KLVPair[]{}), null, mListener)
                    // Set the action buttons
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }

}
