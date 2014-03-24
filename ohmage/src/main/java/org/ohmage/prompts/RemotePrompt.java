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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.app.R;

import java.util.Iterator;

/**
 * Created by cketcham on 1/24/14.
 */
public class RemotePrompt extends AnswerablePrompt {
    String uri;

    @Override
    public SurveyItemFragment getFragment() {
        return RemotePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class RemotePromptFragment extends PromptLauncherFragment<RemotePrompt> {

        private static final String TAG = RemotePromptFragment.class.getSimpleName();

        private static final int ACTIVITY_FINISHED = 0;

        public static RemotePromptFragment getInstance(RemotePrompt prompt) {
            RemotePromptFragment fragment = new RemotePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override protected String getLaunchButtonText() {
            return getString(R.string.launch);
        }

        @Override public void onClick(View v) {
            startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(getPrompt().uri)),
                    ACTIVITY_FINISHED);
        }

        /**
         * If the 'resultCode' indicates failure then we treat it as if the user
         * has skipped the prompt. If skipping is not allowed, we log it as an
         * error, but we do not make an entry in the results array to prevent
         * corrupting it nor do we set as skipped to prevent us from corrupting
         * the entire survey.
         * <p/>
         * If the 'resultCode' indicates success then we check to see what was
         * returned via the parameterized 'data' object. If 'data' is null, we put
         * an empty JSONObject in the array to indicate that something went wrong.
         * If 'data' is not null, we get all the key-value pairs from the data's
         * extras and place them in a JSONObject. If the keys for these extras are
         * certain "special" return codes, some of which are required, then we
         * handle those as well which may or may not include putting them in the
         * JSONObject. Finally, we put the JSONObject in the JSONArray that is the
         * return value for this prompt type.
         */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    JSONArray responseArray = new JSONArray();
                    JSONObject currResponse = new JSONObject();
                    Bundle extras = data.getExtras();
                    Iterator<String> keysIter = extras.keySet().iterator();
                    while (keysIter.hasNext()) {
                        String nextKey = keysIter.next();
                        try {
                            currResponse.put(nextKey, extras.get(nextKey));
                        } catch (JSONException e) {
                            Log.e(TAG, "Invalid return value from remote Activity for key: " +
                                       nextKey);
                        }
                        responseArray.put(currResponse);
                    }
                    setValue(responseArray);
                    return;
                }
            }
            //TODO: return some error?
            setValue(new JSONObject());
        }
    }
}
