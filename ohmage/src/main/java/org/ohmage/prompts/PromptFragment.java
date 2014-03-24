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

/**
 * Created by cketcham on 1/23/14.
 */
public class PromptFragment<T extends Prompt> extends SurveyItemFragment {

    private T prompt;

    public void setPrompt(T prompt) {
        this.prompt = prompt;
    }

    public T getPrompt() {
        return prompt;
    }

    @Override
    protected boolean isSkippable() {
        return prompt.isSkippable();
    }

    @Override protected String getPromptText() {
        return prompt.getText();
    }
}
