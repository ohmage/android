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

import org.ohmage.app.SurveyActivity;

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
        return skippable;
    }

    public boolean hasValidResponse() {
        return value != null;
    }

    public Object getAnswer() {
        return value;
    }

    public Object getAnswerExtras() {
        return null;
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
            if (object == null ? getPrompt().value != null : !object.equals(getPrompt().value)) {
                boolean notify = skippableStateChanged(getPrompt().value, object);
                getPrompt().value = object;
                if (notify) {
                    updateCanContinue();
                }

                // If this prompt as already been answered, we should update the value immediately
                if (isAnswered()) {
                    updateAnswer();
                }
            }
        }

        private void updateAnswer() {
            ((SurveyActivity) getActivity()).getPagerAdapter().updateAnswer(this);
        }

        @Override
        protected boolean canContinue() {
            return getPrompt().hasValidResponse();
        }

        @Override protected void onOkPressed() {
            super.onOkPressed();
            if(getPrompt().hasValidResponse()) {
                updateAnswer();
            }
        }

        @Override protected void onSkipPressed() {
            super.onSkipPressed();
            if (getPrompt().isSkippable()) {
                getPrompt().value=null;
                updateAnswer();
            }
        }

        @Override
        public T getPrompt() {
            return super.getPrompt();
        }
    }
}
