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

import android.text.TextUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.ohmage.app.SurveyActivity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cketcham on 12/19/13.
 */
public class BasePrompt implements Prompt {
    public String surveyItemId;
    public String condition;
    public String text;
    public String surveyItemType;

    @Override
    public boolean isSkippable() {
        return true;
    }

    @Override
    public String getId() {
        return surveyItemId;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    public SurveyItemFragment getFragment() {
        return MessagePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class MessagePromptFragment extends PromptFragment<BasePrompt> {

        public static MessagePromptFragment getInstance(BasePrompt prompt) {
            if (prompt == null || TextUtils.isEmpty(prompt.getText()))
                return null;

            MessagePromptFragment fragment = new MessagePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override protected void onOkPressed() {
            super.onOkPressed();
            ((SurveyActivity) getActivity()).getPagerAdapter().updateAnswer(this);
        }

        @Override
        public boolean isSkippable() {
            return false;
        }
    }

    public static class PromptDeserializer implements JsonDeserializer<Prompt> {
        private static Map<String, Class> map = new HashMap<String, Class>();

        static {
            map.put("message", BasePrompt.class);
            map.put("audio_prompt", AudioPrompt.class);
            map.put("image_prompt", ImagePrompt.class);
            map.put("video_prompt", VideoPrompt.class);
            map.put("number_prompt", NumberPrompt.class);
            map.put("remote_activity_prompt", RemotePrompt.class);
            map.put("number_single_choice_prompt", SingleChoicePrompt.class);
            map.put("string_single_choice_prompt", SingleChoicePrompt.class);
            map.put("number_multi_choice_prompt", MultiChoicePrompt.class);
            map.put("string_multi_choice_prompt", MultiChoicePrompt.class);
            map.put("text_prompt", TextPrompt.class);
            map.put("timestamp_prompt", TimestampPrompt.class);
        }

        public BasePrompt deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) {
            JsonObject object = json.getAsJsonObject();
            String type = object.get("survey_item_type").getAsString();
            if (map.containsKey(type)) {
                BasePrompt prompt = context.deserialize(json, map.get(type));
                if(prompt instanceof AnswerablePrompt) {
                    ((AnswerablePrompt) prompt).value = ((AnswerablePrompt) prompt).defaultResponse;
                }
                return prompt;
            }
            return null;
        }
    }

}
