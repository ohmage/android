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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.UserRecoverableAuthException;

import org.apache.http.auth.AuthenticationException;
import org.mockito.stubbing.OngoingStubbing;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil.GrantType;
import org.ohmage.models.AccessToken;
import org.ohmage.test.dagger.InjectedAndroidTestCase;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.RetrofitError;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * Tests the {@link org.ohmage.auth.Authenticator} code
 */
public class AuthenticatorTest extends InjectedAndroidTestCase {

    @Inject AuthHelper fakeAuthHelper;
    @Inject AccountManager fakeAccountManager;
    @Inject OhmageService fakeOhmageService;

    private Authenticator mAuthenticator;
    private Context fakeContext;
    private Account fakeAccount;
    private static final String fakeGoogleEmail = "fake@gmail.com";
    private static final String accessToken = "token";
    private static final String refreshToken = "refresh";
    private AccessToken token;
    private static final String stubGoogleToken = "google_token";
    private static final IOException ioe = new IOException();
    private static final RetrofitError networkError = RetrofitError.networkError("", ioe);
    private static final RetrofitError serverError = RetrofitError.networkError("", ioe);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeAccount = new Account("name", AuthUtil.AUTHTOKEN_TYPE);
        token = new AccessToken(accessToken, refreshToken, fakeAccount.name);

        fakeContext = mock(Context.class);

        mAuthenticator = new Authenticator(fakeContext);
    }

    public void testGetAuthToken_whenCached_returnsToken() throws Exception {
        setAuthTokenCached(true);

        Bundle bundle = mAuthenticator.getAuthToken(null, fakeAccount,
                AuthUtil.AUTHTOKEN_TYPE, null);

        verify(fakeAccountManager).peekAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE);
        verifyNoMoreInteractions(fakeAccountManager);
        verifyTokenInBundle(bundle);
    }

    public void testGetAuthToken_whenNotCached_usesRefreshToken() throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenSuccess(refreshToken);

        mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        // Get the refresh password
        verify(fakeAccountManager).getPassword(fakeAccount);
        // Use the refresh password to get new tokens
        verify(fakeOhmageService).getAccessToken(refreshToken);
    }

    public void testGetAuthToken_whenNotCached_savesNewRefreshToken() throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenSuccess(refreshToken);

        mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        // Saves the refresh token
        verify(fakeAccountManager).setPassword(fakeAccount, refreshToken);
    }

    public void testGetAuthToken_whenUsingRefreshToken_returnsToken() throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenSuccess(refreshToken);

        Bundle bundle = mAuthenticator.getAuthToken(null, fakeAccount,
                AuthUtil.AUTHTOKEN_TYPE, null);
        verifyTokenInBundle(bundle);
    }

    public void testGetAuthToken_noConnectionErrorUsingRefreshToken_throwsNetworkErrorException()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, networkError);

        try {
            mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof NetworkErrorException);
        }
    }

    public void testGetAuthToken_serverErrorUsingRefreshToken_throwsNetworkErrorException()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, serverError);

        try {
            mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof NetworkErrorException);
        }
    }

    public void testGetAuthToken_authErrorUsingRefreshToken_notifiesUser() throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, new AuthenticationException(""));

        Bundle data = mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        verifyNotifyUserBundle(data);
    }

    public void testGetAuthToken_authErrorUsingRefreshTokenAndGoogleAccount_triesToGetGoogleAuth()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        when(fakeOhmageService.getAccessToken(refreshToken))
                .thenThrow(new AuthenticationException("")).thenReturn(token);
        setHasGoogleAccount(true);
        setGetGoogleAuthTokenResult(null);

        mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        verify(fakeAccountManager).getUserData(fakeAccount, Authenticator.USER_DATA_GOOGLE_ACCOUNT);
        verify(fakeAuthHelper).googleAuthGetToken(fakeGoogleEmail);
    }

    public void testGetAuthToken_userRecoverableErrorWhenAuthtokenFromGoogle_sendsErrorViaIntent()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, new AuthenticationException(""));
        setHasGoogleAccount(true);
        UserRecoverableAuthException fakeException = new UserRecoverableAuthException("msg",
                new Intent());
        setGetGoogleAuthTokenResult(fakeException);


        Bundle data = mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        Intent intent = data.getParcelable(AccountManager.KEY_INTENT);
        assertNotNull(intent);
        assertEquals(intent.getComponent().getClassName(), AuthenticatorActivity.class.getName());
        assertEquals(fakeException, intent
                .getSerializableExtra(AuthenticatorActivity.EXTRA_HANDLE_USER_RECOVERABLE_ERROR));
    }

    public void testGetAuthToken_whenAuthWithGoogle_returnsToken() throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, new AuthenticationException(""));
        setHasGoogleAccount(true);
        setGetGoogleAuthTokenResult(null);
        setAccessTokenSuccess(AuthUtil.GrantType.GOOGLE_OAUTH2, stubGoogleToken);

        Bundle data = mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);

        verifyTokenInBundle(data);
    }

    public void testGetAuthToken_noConnectionErrorWhenAuthWithGoogle_throwsNetworkErrorException()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, new AuthenticationException(""));
        setHasGoogleAccount(true);
        setGetGoogleAuthTokenResult(null);
        setAccessTokenFailure(AuthUtil.GrantType.GOOGLE_OAUTH2, stubGoogleToken, networkError);

        try {
            mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof NetworkErrorException);
        }
    }

    public void testGetAuthToken_serverErrorWhenAuthWithGoogleToken_throwsNetworkException()
            throws Exception {
        setAuthTokenCached(false);
        setAccountRefreshToken();
        setAccessTokenFailure(refreshToken, new AuthenticationException(""));
        setHasGoogleAccount(true);
        setGetGoogleAuthTokenResult(null);
        setAccessTokenFailure(AuthUtil.GrantType.GOOGLE_OAUTH2, stubGoogleToken, serverError);

        try {
            mAuthenticator.getAuthToken(null, fakeAccount, AuthUtil.AUTHTOKEN_TYPE, null);
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof NetworkErrorException);
        }
    }

    private void setAuthTokenCached(boolean cached) {
        when(fakeAccountManager.peekAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE))
                .thenReturn(cached ? accessToken : null);
    }

    private void setAccountRefreshToken() {
        when(fakeAccountManager.getPassword(fakeAccount)).thenReturn(refreshToken);
    }

    private void setAccessTokenSuccess(String refreshToken) throws Exception {
        setAccessTokenResult(refreshToken, null);
    }

    private void setAccessTokenFailure(String refreshToken, Throwable error) throws Exception {
        setAccessTokenResult(refreshToken, error);
    }

    private void setAccessTokenResult(String refreshToken, Throwable error) throws Exception {
        OngoingStubbing<AccessToken> when = when(fakeOhmageService.getAccessToken(refreshToken));
        if (error == null)
            when.thenReturn(token);
        else
            when.thenThrow(error);
    }

    private void setAccessTokenSuccess(GrantType type, String token) throws Exception {
        setAccessTokenResult(type, token, null);
    }

    private void setAccessTokenFailure(GrantType type, String token, Throwable error)
            throws Exception {
        setAccessTokenResult(type, token, error);
    }

    private void setAccessTokenResult(GrantType type, String t, Throwable error) throws Exception {
        OngoingStubbing<AccessToken> when = when(fakeOhmageService.getAccessToken(type, t));
        if (error == null)
            when.thenReturn(token);
        else
            when.thenThrow(error);
    }

    private void setHasGoogleAccount(boolean hasAccount) {
        when(fakeAccountManager.getUserData(fakeAccount, Authenticator.USER_DATA_GOOGLE_ACCOUNT))
                .thenReturn(fakeGoogleEmail);
    }

    private void setGetGoogleAuthTokenResult(Exception e) throws Exception {
        OngoingStubbing<String> stub = when(fakeAuthHelper.googleAuthGetToken(fakeGoogleEmail));
        if (e == null) {
            stub.thenReturn(stubGoogleToken);
        } else {
            stub.thenThrow(e);
        }
    }

    private void verifyTokenInBundle(Bundle bundle) {
        assertEquals(accessToken, bundle.getString(AccountManager.KEY_AUTHTOKEN));
    }

    private void verifyNotifyUserBundle(Bundle bundle) {
        Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
        assertNotNull(intent);
        assertEquals(intent.getComponent().getClassName(), AuthenticatorActivity.class.getName());
        assertTrue(intent.getBooleanExtra(AuthenticatorActivity.EXTRA_FROM_AUTHENTICATOR, false));
    }
}
