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
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.ohmage.app.R;
import org.ohmage.fragments.TransitionFragment;
import org.ohmage.models.User;
import org.ohmage.requests.AccessTokenRequest;
import org.ohmage.requests.CreateUserRequest;

import javax.inject.Inject;

/**
 * Activity which gets details for account creation from the user
 */
public class CreateAccountFragment extends TransitionFragment {

    private static final String TAG = CreateAccountFragment.class.getSimpleName();

    @Inject RequestQueue requestQueue;

    @Inject Bus bus;

    // UI references.
    private EditText mFullnameView;

    private EditText mUsernameView;

    private EditText mEmailView;

    private EditText mPasswordView;

    private User mUser = new User();

    /**
     * Stores the password for the user if it is an ohmage account
     */
    private String mPassword;

    /**
     * The grant type should be set to determine which views will be shown to the user.
     */
    private AuthUtil.GrantType mGrantType;

    /**
     * Callbacks for the activity to handle create button pressed
     */
    private Callbacks mCallbacks;

    public CreateAccountFragment() {
        setDefaultAnimation(R.anim.slide_in_top, R.anim.slide_out_top);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        bus.register(this);
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
        bus.unregister(this);
        mCallbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_account, container, false);

        // Set up the login form.
        mEmailView = (EditText) view.findViewById(R.id.email);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mFullnameView = (EditText) view.findViewById(R.id.fullname);

        mUsernameView = (EditText) view.findViewById(R.id.username);
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.create_account || id == EditorInfo.IME_NULL) {
                    attemptAccountCreate();
                    return true;
                }
                return false;
            }
        });

        mFullnameView.setText(mUser.fullName);

        view.findViewById(R.id.create_account_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAccountCreate();
            }
        });

        setViewState(view);

        return view;
    }

    /**
     * Sets the visibility state of the prompts based on the grant type
     *
     * @param view
     */
    private void setViewState(View view) {
        if (view != null) {
            view.findViewById(R.id.ohmage_account_prompts).setVisibility(
                    (showOhmageAccountPrompts()) ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.header_text)).setText((showOhmageAccountPrompts())
                    ? R.string.create_ohmage_account_title
                    : R.string.common_signin_button_text_long);
        }
    }

    /**
     * Called when the user clicks the create button. This will callback to the activity if
     * the values supplied by the user are valid.
     */
    public void attemptAccountCreate() {

        mUsernameView.setError(null);

        String fullName = mFullnameView.getText().toString();
        String username = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (username.length() < 4) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        } else if (showOhmageAccountPrompts() && TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (showOhmageAccountPrompts() && TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (showOhmageAccountPrompts() && !isValidEmail(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email_address));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            mUser.fullName = fullName;
            mUser.username = username;
            mUser.email = email;

            // First notify the activity that the account is being created
            if (mCallbacks != null) {
                mCallbacks.onCreateAccount();
            }

            // Either ask for the token or start to create the account
            if (!showOhmageAccountPrompts()) {
                mCallbacks.fetchToken(new UseToken() {
                    @Override
                    public void useToken(String token) {
                        createAccount(token);
                    }
                });
            } else {
                createAccount(mPassword);
            }
        }
    }

    /**
     * Just a simple check for now to see if there is an at sign and a period
     *
     * @param email
     * @return true if the email is valid enough..
     */
    private boolean isValidEmail(String email) {
        return email.matches(".*@.*\\..*");
    }

    /**
     * Make the network call to create a user account
     *
     * @param token
     */
    private void createAccount(String token) {
        requestQueue.add(new CreateUserRequest(mGrantType, token, mUser));
    }

    /**
     * Set the default full name value
     *
     * @param fullName
     */
    public void setFullName(String fullName) {
        if (!TextUtils.isEmpty(fullName))
            mUser.fullName = fullName;
    }

    /**
     * Check if we should show the ohmage account specific prompts
     *
     * @return true if we are creating an ohmage account
     */
    private boolean showOhmageAccountPrompts() {
        return mGrantType == AuthUtil.GrantType.CLIENT_CREDENTIALS;
    }

    /**
     * This function is called when the {@link org.ohmage.requests.CreateUserRequest} finishes
     * successfully
     *
     * @param user
     */
    @Subscribe
    public void onUserCreatedRequest(User user) {
        if (mGrantType == AuthUtil.GrantType.CLIENT_CREDENTIALS) {
            requestQueue.add(new AccessTokenRequest(user.username, mPassword));
        } else {
            mCallbacks.fetchToken(new UseToken() {
                @Override
                public void useToken(String token) {
                    requestQueue.add(new AccessTokenRequest(mGrantType, token));
                }
            });
        }
    }

    /**
     * Set the grant type for this fragment
     *
     * @param grantType
     */
    public void setGrantType(AuthUtil.GrantType grantType) {
        mGrantType = grantType;
        setViewState(getView());
    }

    public static interface Callbacks {
        /**
         * Called when the user clicks the create account button if we are performing an action
         * which requires the calling activity to fetch the token. The parent activity must
         * call {@link #createAccount(String)} with the token.
         */
        void fetchToken(UseToken useToken);

        /**
         * Called when the account is actually being created
         */
        void onCreateAccount();
    }

    public static interface UseToken {
        /**
         * A callback that the activity can use to send a token to this fragment
         *
         * @param token
         */
        void useToken(String token);
    }
}
