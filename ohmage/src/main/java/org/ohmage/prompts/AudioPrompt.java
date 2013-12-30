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

import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.ohmage.app.R;
import org.ohmage.widget.AudioRecorder;
import org.ohmage.widget.AudioRecorder.RecordStateListener;

/**
 * Created by cketcham on 1/24/14.
 */
public class AudioPrompt extends MediaPrompt {

    @Override
    public Fragment getFragment() {
        return AudioPromptFragment.getInstance(this);
    }

    public static class AudioPromptFragment extends AnswerablePromptFragment<AudioPrompt>
            implements AudioRecorder.OnErrorListener {

        public static AudioPromptFragment getInstance(AudioPrompt prompt) {
            AudioPromptFragment fragment = new AudioPromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

//        @Override
//        protected void setEnabled(ViewGroup view, boolean enabled) {
//            super.setEnabled(view, enabled);
//            if (view == null)
//                return;
//            AudioRecorder recorder = (AudioRecorder) view.findViewById(R.id.audio_recorder);
//            recorder.setEnabled(enabled);
//            recorder.setClickable(enabled);
//            recorder.setLongClickable(enabled);
//        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.prompt_audio, container, true);

            AudioRecorder recorder = (AudioRecorder) view.findViewById(R.id.audio_recorder);
            recorder.setMaxDuration(getPrompt().maxDuration);
            recorder.setMediaRecorderOnInfoListener(this);
            recorder.setFile(getPrompt().getFile());
            recorder.setRecordStateListener(new RecordStateListener() {
                @Override public void onRecordStateChanged(AudioRecorder recorder, int state) {
                    if (state == AudioRecorder.STATE_STOPPED) {
                        notifySkipStateChanged();

                        // We can stop listening now since the file will always exist from now on
                        recorder.setRecordStateListener(null);
                    }
                }
            });
        }

        @Override
        public void onUnableToAccessMedia(AudioRecorder recorder) {
            Toast.makeText(getActivity(), R.string.media_unavailable_to_record_audio,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            int msg = -1;
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                msg = R.string.audio_prompt_max_duration_reached;
            } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                msg = R.string.audio_prompt_max_filesize_reached;
            }

            if (msg != -1)
                Toast.makeText(getActivity(), getString(msg), Toast.LENGTH_SHORT).show();
        }
    }
}
