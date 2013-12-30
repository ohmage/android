/*
 * Copyright (C) 2013 ohmage
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

package org.ohmage.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.SignInButton;

import org.ohmage.app.R;
import org.ohmage.fragments.TransitionFragment;

/**
 * This fragment shows the sign in buttons and loading state for the
 * {@link org.ohmage.auth.AuthenticatorActivity}
 */
public class AuthenticateFragment extends TransitionFragment implements View.OnClickListener {

    /**
     * Keeps track of the state of if the progress spinner is shown
     */
    private boolean mShowProgress;

    /**
     * The view which shows the progress spinner
     */
    private View mAuthLoadingView;

    /**
     * The view which shows the authentication buttons
     */
    private View mAuthButtonsView;

    private Callbacks mCallbacks;

    public AuthenticateFragment() {
        setDefaultAnimation(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_authenticate, container, false);

        SignInButton signInButton = (SignInButton) view.findViewById(R.id.sign_in_google_button);
        signInButton.setOnClickListener(this);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        Button createAccount = (Button) view.findViewById(R.id.create_account_button);
        createAccount.setOnClickListener(this);
        Button signInEmail = (Button) view.findViewById(R.id.sign_in_email_button);
        signInEmail.setOnClickListener(this);

        mAuthLoadingView = view.findViewById(R.id.authenticate_loading);
        mAuthButtonsView = view.findViewById(R.id.authenticate_buttons);

        // Set the visibility of the views
        mAuthLoadingView.setVisibility(mShowProgress ? View.VISIBLE : View.INVISIBLE);
        mAuthButtonsView.setVisibility(mShowProgress ? View.INVISIBLE : View.VISIBLE);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showProgress", mShowProgress);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    /**
     * Shows the progress UI and hides the authentication buttons.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(boolean show) {
        mShowProgress = show;

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mAuthLoadingView.setVisibility(View.VISIBLE);
            mAuthLoadingView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAuthLoadingView.setVisibility(
                                    mShowProgress ? View.VISIBLE : View.INVISIBLE);
                        }
                    });

            mAuthButtonsView.setVisibility(View.VISIBLE);
            mAuthButtonsView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAuthButtonsView.setVisibility(
                                    mShowProgress ? View.INVISIBLE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply showProgress
            // and hide the relevant UI components.
            mAuthLoadingView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            mAuthButtonsView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_google_button:
                mCallbacks.onGoogleSignInClick();
                break;
            case R.id.create_account_button:
                mCallbacks.onCreateAccountClick();
                break;
            case R.id.sign_in_email_button:
                mCallbacks.onEmailSignInClick();
                break;
        }
    }

    public static interface Callbacks {
        void onGoogleSignInClick();

        void onCreateAccountClick();

        void onEmailSignInClick();
    }
}