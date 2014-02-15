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

import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmageService.CancelableCallback;
import org.ohmage.app.R;
import org.ohmage.fragments.TransitionFragment;
import org.ohmage.models.AccessToken;
import org.ohmage.models.User;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Activity which gets details for account creation from the user
 */
public class CreateAccountFragment extends TransitionFragment {

    private static final String TAG = CreateAccountFragment.class.getSimpleName();

    @Inject OhmageService ohmageService;

    // UI references.
    private EditText mFullnameView;
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
        View view = inflater.inflate(R.layout.fragment_create_account, container, false);

        // Set up the login form.
        mFullnameView = (EditText) view.findViewById(R.id.fullname);
        mEmailView = (EditText) view.findViewById(R.id.email);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.create_account || id == EditorInfo.IME_NULL) {
                    attemptAccountCreate();
                    return true;
                }
                return false;
            }
        });

        mEmailView.setText(mUser.email);
        mFullnameView.setText(mUser.fullName);

        view.findViewById(R.id.create_account_button)
            .setOnClickListener(new View.OnClickListener() {
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

        mEmailView.setError(null);

        String fullName = mFullnameView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email.
        if (showOhmageAccountPrompts() && TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (showOhmageAccountPrompts() && !isValidEmail(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email_address));
            focusView = mEmailView;
            cancel = true;
        } else if (showOhmageAccountPrompts() && TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            mUser.fullName = TextUtils.isEmpty(fullName) ? null : fullName;
            mUser.email = TextUtils.isEmpty(email) ? null : email;

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
    private void createAccount(final String token) {
        if (mGrantType == AuthUtil.GrantType.CLIENT_CREDENTIALS) {
            CancelableCallback<User> callback = new CancelableCallback<User>() {
                @Override public void success(User user, Response response) {
                    ((AuthenticatorActivity) getActivity()).createAccount(user, token);
                }

                @Override public void failure(RetrofitError error) {
                    ((AuthenticatorActivity) getActivity()).onRetrofitError(error);
                }
            };

            String emailVerificationCode = getActivity().getIntent().getStringExtra(
                    AuthenticatorActivity.EXTRA_USER_INVITATION_CODE);
            if (emailVerificationCode != null)
                ohmageService.createUser(token, mUser, emailVerificationCode, callback);
            else
                ohmageService.createUser(token, mUser, callback);
        } else {
            ohmageService.createUser(mGrantType, token, mUser,
                    new OhmageService.CancelableCallback<User>() {
                        @Override public void success(final User user, Response response) {
                            mCallbacks.fetchToken(new UseToken() {
                                @Override
                                public void useToken(String token) {
                                    ohmageService.getAccessToken(mGrantType, token,
                                            new OhmageService.CancelableCallback<AccessToken>() {
                                                @Override
                                                public void success(AccessToken accessToken,
                                                        Response response) {
                                                    ((AuthenticatorActivity) getActivity())
                                                            .createAccount(user.email,
                                                                    accessToken);
                                                }

                                                @Override public void failure(RetrofitError error) {
                                                    ((AuthenticatorActivity) getActivity())
                                                            .onRetrofitError(error);
                                                }
                                            });
                                }
                            });
                        }

                        @Override public void failure(RetrofitError error) {
                            ((AuthenticatorActivity) getActivity()).onRetrofitError(error);
                        }
                    });
        }
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
     * Set the grant type for this fragment
     *
     * @param grantType
     */
    public void setGrantType(AuthUtil.GrantType grantType) {
        mGrantType = grantType;
        setViewState(getView());
    }

    public void setEmail(String email) {
        if (!TextUtils.isEmpty(email))
            mUser.email = email;
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
