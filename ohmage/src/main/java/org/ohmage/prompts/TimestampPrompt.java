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
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by cketcham on 1/23/14.
 */
public class TimestampPrompt extends AnswerablePrompt<Calendar> {


    @Override
    public SurveyItemFragment getFragment() {
        return TimestampPromptFragment.getInstance(this);
    }

    public void addAnswer(JSONObject data, JSONObject extras) throws JSONException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        data.put(surveyItemId, formatter.format(value.getTime()));
    }

    public static class TimestampPromptFragment extends AnswerablePromptFragment<TimestampPrompt> {

        public static TimestampPromptFragment getInstance(TimestampPrompt prompt) {
            TimestampPromptFragment fragment = new TimestampPromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_timestamp, container, true);

            DatePicker datePicker = (DatePicker) view.findViewById(R.id.datePicker);
            Calendar c = Calendar.getInstance();
            setValue(c);
            datePicker
                    .init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                            new DatePicker.OnDateChangedListener() {
                                @Override
                                public void onDateChanged(DatePicker view, int year,
                                        int monthOfYear, int dayOfMonth) {
                                    Calendar c = getPrompt().value;
                                    c.set(Calendar.YEAR, year);
                                    c.set(Calendar.MONTH, monthOfYear);
                                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    setValue(c);
                                }
                            });
            TimePicker timePicker = (TimePicker) view.findViewById(R.id.timePicker);
            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    Calendar c = getPrompt().value;
                    c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    c.set(Calendar.MINUTE, minute);
                    setValue(c);
                }
            });
        }
    }
}
