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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.ohmage.app.R;
import org.ohmage.app.Util;
import org.ohmage.models.Stream.RemoteApp;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by cketcham on 1/24/14.
 */
public class RemotePrompt extends AnswerablePrompt {
    public String uri;

    @SerializedName("apps")
    @Expose
    private RemoteApp app;

    @Override
    public SurveyItemFragment getFragment() {
        return RemotePromptFragment.getInstance(this);
    }

    @Nullable
    public RemoteApp getApp() {
        return app == null || !app.appDefinitionExists() ? null : app;
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
            if (getPrompt().app == null) {
                Toast.makeText(getActivity(),
                        getString(R.string.remote_prompt_missing_app_definition),
                        Toast.LENGTH_SHORT).show();
            } else if (getPrompt().canLaunchPrompt(getActivity())) {
                startActivityForResult(getPrompt().getLaunchIntent(), ACTIVITY_FINISHED);
            } else {
                Toast.makeText(getActivity(), getString(R.string.remote_prompt_launch_error),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override public void onResume() {
            super.onResume();

            if (getPrompt().app == null) {
                // This prompt is incorrectly set up
                return;
            }

            if (getView() != null) {
                if (!getPrompt().app.isInstalled(getActivity())) {
                    Button launch = (Button) getView().findViewById(R.id.launch);
                    launch.setText("Install");
                    launch.setOnClickListener(new OnClickListener() {
                        @Override public void onClick(View v) {
                            if (!Util.safeStartActivity(getActivity(),
                                    getPrompt().app.installIntent())) {
                                Toast.makeText(getActivity(), "No app can be found to install",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Button launch = (Button) getView().findViewById(R.id.launch);
                    launch.setText(getLaunchButtonText());
                    launch.setOnClickListener(this);
                }
            }
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
            HashMap<String, Object> response = new HashMap<String, Object>();
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Iterator<String> keysIter = extras.keySet().iterator();
                    while (keysIter.hasNext()) {
                        String nextKey = keysIter.next();
                        response.put(nextKey, extras.get(nextKey));
                    }
                }
            }
            //TODO: return some error?
            setValue(response);
        }
    }

    private Intent getLaunchIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    }

    public boolean canLaunchPrompt(Context context) {
        return Util.activityExists(context, getLaunchIntent());
    }
}
