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
import org.ohmage.app.Ohmage;

import java.io.File;
import java.util.UUID;

/**
 * Created by cketcham on 1/24/14.
 */
public class MediaPrompt extends AnswerablePrompt<File> {
    public int maxDuration;

    public long maxDimension;

    @Override
    public boolean isSkippable() {
        return skippable || (value != null && value.exists());
    }

    /**
     * Returns or generates a file to be used for this response
     *
     * @return
     */
    public File getFile() {
        if (value != null)
            return value;

        value = new File(Ohmage.app().getExternalCacheDir(), UUID.randomUUID().toString());
        return value;
    }

    @Override
    public void addAnswer(JSONObject data, JSONObject extras) throws JSONException {
        data.put(surveyItemId, value.getName());
        extras.put(surveyItemId, value.getAbsolutePath());
    }
}
