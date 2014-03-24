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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by cketcham on 12/19/13.
 * TODO: value goes here? or elsewhere
 */
public abstract class AnswerablePrompt<T> extends BasePrompt {
    public T value;

    public T defaultResponse;

    public boolean skippable = false;

    public AnswerablePrompt() {
        value = defaultResponse;
    }

    public boolean isSkippable() {
        return skippable || value != null;
    }

    public boolean hasValidResponse() {
        return value != null;
    }

    public void addAnswer(JSONObject data, JSONObject extras) throws JSONException {
        data.put(surveyItemId, value);
    }

    public static class AnswerablePromptFragment<T extends AnswerablePrompt>
            extends PromptFragment<T> {

        /**
         * Calculates if the skippable state between two objects might have changed. Typically this occurs when a value is set or cleared.
         *
         * @param o
         * @param n
         * @return
         */
        protected boolean skippableStateChanged(Object o, Object n) {
            return (o != null && !o.equals(n)) || (n != null && !n.equals(o));
        }

        protected void setValue(Object object) {
            boolean notify = skippableStateChanged(getPrompt().value, object);
            getPrompt().value = object;
            if (notify) {
                updateCanContinue();
            }
        }

        public interface OnValidAnswerStateChangedListener {
            void onValidAnswerStateChanged(AnswerablePrompt prompt);
        }

        private OnValidAnswerStateChangedListener mOnValidAnswerStateChangedListener;

        public void setOnValidAnswerStateChangedListener(
                OnValidAnswerStateChangedListener onValidAnswerStateChangedListener) {
            mOnValidAnswerStateChangedListener = onValidAnswerStateChangedListener;
        }

        private void notifyValidAnswerStateChanged() {
            if (mOnValidAnswerStateChangedListener != null) {
                mOnValidAnswerStateChangedListener.onValidAnswerStateChanged(getPrompt());
            }
        }

        @Override
        protected boolean canContinue() {
            return getPrompt().hasValidResponse();
        }

        @Override protected void onOkPressed() {
            super.onOkPressed();
            if(getPrompt().hasValidResponse()) {
                notifyValidAnswerStateChanged();
            }
        }

        @Override protected void onSkipPressed() {
            super.onSkipPressed();
            if (getPrompt().isSkippable()) {
                notifyValidAnswerStateChanged();
            }
        }

        @Override public boolean isAnswered() {
            return super.isAnswered() && (getPrompt().hasValidResponse() || getPrompt().isSkippable());
        }

        @Override
        public T getPrompt() {
            return super.getPrompt();
        }
    }
}
