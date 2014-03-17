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

package org.ohmage.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import org.ohmage.app.R;

import java.io.File;
import java.io.IOException;

/**
 * Created by cketcham on 1/27/14.
 */
public class AudioRecorder extends LinearLayout {

    private static final String TAG = "AudioRecorder";

    private String mFileName = null;

    private ToggleButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private ToggleButton mPlayButton = null;
    private MediaPlayer mPlayer = null;

    private int mMaxDuration = 0;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_PLAYING = 2;
    private RecordStateListener mRecordStateListener;

    public interface RecordStateListener {
        public void onRecordStateChanged(AudioRecorder recorder, int state);
    }

    public interface OnErrorListener extends MediaRecorder.OnInfoListener {
        public void onUnableToAccessMedia(AudioRecorder recorder);
    }

    private OnErrorListener mOnErrorListener;

    public AudioRecorder(Context context) {
        this(context, null);
    }

    public AudioRecorder(Context context, AttributeSet attrs) {
        super(context, attrs);

        setGravity(Gravity.CENTER);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_audio_recorder, this, true);

        boolean checked = (mRecordButton != null) ? mRecordButton.isChecked() : false;
        mRecordButton = (ToggleButton) findViewById(R.id.record_button);
        mRecordButton.setChecked(checked);
        mRecordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TODO: why can I click this button if it is disabled? maybe the setFragment sets this to be clickable again after it is disabled?
                onRecord(mRecordButton.isChecked());
                ensurePlayButtonState();
            }
        });

        checked = (mPlayButton != null) ? mPlayButton.isChecked() : false;
        mPlayButton = (ToggleButton) findViewById(R.id.play_button);
        mPlayButton.setChecked(checked);
        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onPlay(mPlayButton.isChecked());
            }
        });
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
            unableToAccessMedia();
            return;
        }
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                    mPlayButton.setChecked(false);
                }
            });
            dispatchPlayingState();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        dispatchStoppedState();
    }

    private void startRecording() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            unableToAccessMedia();
            return;
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // Set max durations
        mRecorder.setMaxDuration(mMaxDuration);
        mRecorder.setMaxFileSize(300 * 1024 * 1024);
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {

                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        onRecord(false);
                        mRecordButton.setChecked(false);
                        ensurePlayButtonState();
                        break;
                }
                if (mOnErrorListener != null)
                    mOnErrorListener.onInfo(mr, what, extra);
            }
        });

        try {
            mRecorder.prepare();
            dispatchRecordingState();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        dispatchStoppedState();
    }

    private void unableToAccessMedia() {
        mRecordButton.setChecked(false);
        mPlayButton.setChecked(false);
        dispatchOnUnableToAccessMedia();
    }

    public void setMaxDuration(int maxDuration) {
        mMaxDuration = maxDuration;
    }

    public void setFile(File file) {
        mFileName = file.getAbsolutePath();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensurePlayButtonState();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !isEnabled();
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            mPlayButton.setEnabled(false);
        } else {
            ensurePlayButtonState();
        }
    }

    @Override public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        mPlayButton.setFocusable(focusable);
    }

    @Override public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        mPlayButton.setClickable(clickable);
    }

    @Override public void setLongClickable(boolean longClickable) {
        super.setLongClickable(longClickable);
        mPlayButton.setLongClickable(longClickable);
    }

    private void ensurePlayButtonState() {
        mPlayButton.setEnabled(!mRecordButton.isChecked() && new File(mFileName).exists());
    }

    public void setRecordStateListener(RecordStateListener listener) {
        mRecordStateListener = listener;
    }

    private void dispatchPlayingState() {
        if (mRecordStateListener != null)
            mRecordStateListener.onRecordStateChanged(this, STATE_PLAYING);
    }

    private void dispatchRecordingState() {
        if (mRecordStateListener != null)
            mRecordStateListener.onRecordStateChanged(this, STATE_RECORDING);
    }

    private void dispatchStoppedState() {
        if (mRecordStateListener != null)
            mRecordStateListener.onRecordStateChanged(this, STATE_STOPPED);
    }

    public void setMediaRecorderOnInfoListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    private void dispatchOnUnableToAccessMedia() {
        if (mOnErrorListener != null)
            mOnErrorListener.onUnableToAccessMedia(this);
    }
}
