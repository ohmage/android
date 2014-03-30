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
import android.provider.MediaStore;
import android.view.View;

import org.ohmage.app.R;

import java.io.File;

/**
 * Created by cketcham on 1/24/14.
 */
public class ImagePrompt extends MediaPrompt {

    @Override
    public SurveyItemFragment getFragment() {
        return ImagePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class ImagePromptFragment extends PromptLauncherFragment<ImagePrompt> {

        private static final int REQUEST_CODE = 0;
        private File mFile;

        public static ImagePromptFragment getInstance(ImagePrompt prompt) {
            ImagePromptFragment fragment = new ImagePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override protected String getLaunchButtonText() {
            return getString(R.string.take_image);
        }

        @Override public void onClick(View v) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mFile = MediaPrompt.getTemporaryResponseFile();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
            startActivityForResult(intent, REQUEST_CODE);
        }

        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == Activity.RESULT_OK) {
                //TODO: resize image
                setValue(mFile);
            }
        }

        @Override protected void onSkipPressed() {
            super.onSkipPressed();
            if(mFile != null) {
                mFile.delete();
            }
        }
    }
}
