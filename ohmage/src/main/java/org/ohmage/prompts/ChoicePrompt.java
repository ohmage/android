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
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;

import org.ohmage.app.R;

import java.util.ArrayList;

/**
 * Created by cketcham on 1/24/14.
 */
public class ChoicePrompt<T> extends AnswerablePrompt<Object> {

    public ArrayList<KLVPair<Object>> choices;

    public static class KLVPair<T> implements CharSequence {
        public String text;

        public T value;

        @Override public int length() {
            String str = text;
            if (str == null) {
                if (value == null) return 0;
                str = value.toString();
            }
            return str.length();
        }

        @Override public char charAt(int index) {
            String str = text;
            if (str == null) {
                if (value == null) throw new IndexOutOfBoundsException();
                str = value.toString();
            }
            return str.charAt(index);
        }

        @Override public CharSequence subSequence(int start, int end) {
            String str = text;
            if (str == null) {
                if (value == null) throw new IndexOutOfBoundsException();
                str = value.toString();
            }
            return str.subSequence(start, end);
        }

        @Override public String toString() {
            String str = text;
            if (str == null) {
                if (value == null) return "";
                str = value.toString();
            }
            return str;
        }
    }

    @Override
    public Fragment getFragment() {
        return SingleChoicePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class SingleChoicePromptFragment<T>
            extends PromptLauncherFragment<ChoicePrompt<T>>
            implements DialogInterface.OnClickListener {

        public static SingleChoicePromptFragment getInstance(ChoicePrompt prompt) {
            SingleChoicePromptFragment fragment = new SingleChoicePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override protected String getLaunchButtonText() {
            return getString(R.string.show_choices);
        }

        @Override public void onClick(View v) {
            SingleChoiceDialogFragment.getInstance(this, getPrompt().choices)
                    .show(getFragmentManager(), "dialog");
        }

        @Override public void onClick(DialogInterface dialog, int which) {
            Object selected = getPrompt().choices.get(which).value;
            if (selected.equals(getPrompt().value))
                setValue(null);
            else
                setValue(selected);
        }
    }

    public static class SingleChoiceDialogFragment extends DialogFragment {

        private OnClickListener mListener;

        public static <T> DialogFragment getInstance(OnClickListener l,
                ArrayList<KLVPair<T>> choices) {
            SingleChoiceDialogFragment fragment = new SingleChoiceDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("choices", choices);
            fragment.setArguments(args);
            fragment.setOnClickListener(l);
            return fragment;
        }

        public void setOnClickListener(OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            ArrayList<KLVPair> choices =
                    (ArrayList<KLVPair>) getArguments().getSerializable("choices");


            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setSingleChoiceItems(choices.toArray(new KLVPair[]{}), -1, mListener)
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
