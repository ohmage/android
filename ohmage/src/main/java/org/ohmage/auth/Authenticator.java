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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.ohmage.app.Ohmage;
import org.ohmage.app.R;
import org.ohmage.models.AccessToken;
import org.ohmage.requests.AccessTokenRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating ohmage accounts
 */
public class Authenticator extends AbstractAccountAuthenticator {
    @Inject AccountManager am;

    @Inject RequestQueue requestQueue;

    @Inject AuthHelper authHelper;

    /**
     * This is the google email of an account that can be used for authentication
     */
    public static final String USER_DATA_GOOGLE_ACCOUNT = "user_data_google_account";

    // Authentication Service context
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType, String authTokenType, String[] requiredFeatures,
                             Bundle options) {
        // TODO: decide if we should allow more than one account, and if not, make it clear to the
        // user that they need to logout and login with a new account

        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account, Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
                                 String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle loginOptions)
            throws NetworkErrorException {

        // Extract the username and refresh_token from the Account Manager, and ask
        // the server for a new refresh_token.
        String authToken = am.peekAuthToken(account, authTokenType);

        // Lets give another try to authenticate the user
        AccessToken token = null;
        UserRecoverableAuthException userRecoverableAuthException = null;

        if (TextUtils.isEmpty(authToken)) {
            final String refreshToken = am.getPassword(account);
            if (refreshToken != null) {

                RequestFuture<AccessToken> future = RequestFuture.newFuture();
                AccessTokenRequest request = new AccessTokenRequest(refreshToken);
                request.setResponseListener(future);
                request.setErrorListener(future);
                future.setRequest(request);
                requestQueue.add(request);

                try {
                    token = future.get(1, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    throw new NetworkErrorException();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof AuthFailureError) {
                        // This will happen if the refresh token was already used, or it was
                        // invalidated or something

                        // We can try getting the token from google
                        String googleAccount = am.getUserData(account, USER_DATA_GOOGLE_ACCOUNT);
                        if (googleAccount != null) {
                            try {
                                token = getTokenFromGoogle(googleAccount);
                            } catch (UserRecoverableAuthException e1) {
                                userRecoverableAuthException = e1;
                            }
                        }
                    } else {
                        throw new NetworkErrorException();
                    }
                }
            }
        }

        // If we get an authToken - we return it
        if (token != null) {
            am.setPassword(account, token.getRefreshToken());
            authToken = token.getAccessToken();
        }

        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticatorActivity.EXTRA_FROM_AUTHENTICATOR, true);
        intent.putExtra(AuthenticatorActivity.EXTRA_HANDLE_USER_RECOVERABLE_ERROR,
                userRecoverableAuthException);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    private AccessToken getTokenFromGoogle(String googleAccount) throws NetworkErrorException, UserRecoverableAuthException {
        // We can check for a google account to automatically update the refresh token

        try {
            String googleAccessToken = authHelper.googleAuthGetToken(googleAccount);

            RequestFuture<AccessToken> future = RequestFuture.newFuture();
            AccessTokenRequest request = new AccessTokenRequest(AuthUtil.GrantType.GOOGLE_OAUTH2,
                    googleAccessToken);
            request.setResponseListener(future);
            request.setErrorListener(future);
            future.setRequest(request);
            requestQueue.add(request);

            return future.get();
        } catch (UserRecoverableAuthException userAuthEx) {
            throw userAuthEx;
        } catch (GoogleAuthException authEx) {
            // We can't really deal with this.. hopefully it doesn't happen
        } catch (InterruptedException e2) {
            // We tried.. Lets just show the authenticator activity
            // TODO: not sure what to do here...
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoConnectionError) {
                throw new NetworkErrorException();
            }
        } catch (IOException transientEx) {
            throw new NetworkErrorException();
        }
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (authTokenType.equals(AuthUtil.AUTHTOKEN_TYPE)) {
            return mContext.getString(R.string.app_name);
        }
        return null;

    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle loginOptions) {
        throw new UnsupportedOperationException();
    }

}