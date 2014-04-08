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

    public static File getTemporaryResponseFile() {
        return new File(Ohmage.app().getExternalCacheDir(), UUID.randomUUID().toString());
    }

    @Override
    public Object getAnswer() {
        return value.getName();
    }

    /**
     * TODO: maybe we don't need the extras json? the name of the file is the prompt value and I know where to look for the file
     */
    @Override
    public String getAnswerExtras() {
        return value.getAbsolutePath();
    }
}
