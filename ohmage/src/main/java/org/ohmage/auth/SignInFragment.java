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

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;

import org.ohmage.app.R;
import org.ohmage.fragments.TransitionFragment;
import org.ohmage.requests.AccessTokenRequest;

import javax.inject.Inject;

/**
 * Activity which attempts to log the user in with their username and password
 */
public class SignInFragment extends TransitionFragment {

    @Inject RequestQueue requestQueue;

    private static final String TAG = SignInFragment.class.getSimpleName();

    // UI references.
    private EditText mUsernameView;

    private EditText mPasswordView;

    /**
     * Callbacks for the activity to handle create button pressed
     */
    private Callbacks mCallbacks;

    private String mUsername;

    public SignInFragment() {
        setDefaultAnimation(R.anim.slide_in_top, R.anim.slide_out_top);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in_ohmage, container, false);

        // Set up the login form.
        mUsernameView = (EditText) view.findViewById(R.id.username);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptSignIn();
                    return true;
                }
                return false;
            }
        });

        if (!TextUtils.isEmpty(mUsername)) {
            mUsernameView.setText(mUsername);
            mUsernameView.setEnabled(false);
        }

        view.findViewById(R.id.sign_in_ohmage_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignIn();
            }
        });

        return view;
    }

    /**
     * Called when the user clicks the create button. This will callback to the activity if
     * the values supplied by the user are valid.
     */
    public void attemptSignIn() {

        mUsernameView.setError(null);

        String password = mPasswordView.getText().toString();
        String username = mUsernameView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            // First notify the activity that the account is being created
            if (mCallbacks != null) {
                mCallbacks.onAccountSignInOhmage();
            }

            // Actually make the request to get the access token
            requestQueue.add(new AccessTokenRequest(username, password));
        }
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public static interface Callbacks {
        /**
         * Called when the account is being signed in
         */
        void onAccountSignInOhmage();
    }
}