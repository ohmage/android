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
import android.test.suitebuilder.annotation.LargeTest;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.google.android.gms.plus.PlusClient;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohmage.app.R;
import org.ohmage.dagger.InjectedActivityInstrumentationTestCase;
import org.ohmage.dagger.PlusClientFragmentTestModule;
import org.ohmage.models.AccessToken;
import org.ohmage.models.User;
import org.ohmage.requests.AccessTokenRequest;
import org.ohmage.requests.CreateUserRequest;
import org.ohmage.test.DeliverVolleyErrorToBus;
import org.ohmage.test.DeliverVolleyResultToBus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isClickable;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Functional Tests for the {@link org.ohmage.auth.AuthenticatorActivity}
 * TODO: Test that the error messages are shown. As of now this is a limitation of Espresso.
 * TODO: details here: https://code.google.com/p/android-test-kit/issues/detail?id=7
 */
@LargeTest
public class AuthenticatorActivityTest extends InjectedActivityInstrumentationTestCase<AuthenticatorActivity> {

    @Inject AuthHelper fakeAuthHelper;

    @Inject AccountManager fakeAccountManager;

    @Inject RequestQueue fakeRequestQueue;

    @Inject PlusClientFragment fakePlusClientFragment;

    private static final String fakeGoogleEmail = "fake@gmail.com";

    private static final String fakeGoogleToken = "google_token";

    private static final String fakeEmail = "fake@email.com";

    private static final String fakePassword = "password";

    private static final String fakeFullname = "Full Name";

    private static final String fakeUsername = "username";

    private static final User fakeUser = new User();

    {
        fakeUser.email = fakeEmail;
        fakeUser.fullName = fakeFullname;
        fakeUser.username = fakeUsername;
    }

    private static final NetworkResponse fakeNetworkResponse = new NetworkResponse("Horrible Error".getBytes());

    private static final ServerError fakeServerError = new ServerError(fakeNetworkResponse);

    public AuthenticatorActivityTest() {
        super(AuthenticatorActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(fakeAccountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE)).thenReturn(new Account[]{});
        getActivity();
    }

    public void testStartingUp_noAccountExists_authButtonsAreVisible() {
        onView(withId(R.id.sign_in_google_button))
                .check(matches(isDisplayed()));

        onView(withId(R.id.sign_in_ohmage_button))
                .check(matches(isDisplayed())).check(matches(isClickable()));

        onView(withId(R.id.create_account_button))
                .check(matches(isDisplayed())).check(matches(isClickable()));
    }

    public void testClicking_signInOhmageButton_showsSignInFragment() {
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.sign_in_ohmage_frame)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_validAccount_getsAccessToken() {
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        verify(fakeRequestQueue).add(new AccessTokenRequest(fakeUsername, fakePassword));
    }

    public void testSignInWithOhmage_invalidAccount_showsSignInWithOhmage() {
        when(fakeRequestQueue.add(new AccessTokenRequest(fakeUsername, fakePassword))).then(
                new DeliverVolleyErrorToBus(new AuthFailureError()));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_ohmage_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_serverError_showsSignInWithOhmage() {
        when(fakeRequestQueue.add(new AccessTokenRequest(fakeUsername, fakePassword))).then(
                new DeliverVolleyErrorToBus(fakeServerError));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_ohmage_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noNetwork_showsSignInWithOhmage() {
        when(fakeRequestQueue.add(new AccessTokenRequest(fakeUsername, fakePassword))).then(
                new DeliverVolleyErrorToBus(new NetworkError()));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_ohmage_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noUsername_doesNotPerformNetworkRequest() {
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        verify(fakeRequestQueue, never()).add(new AccessTokenRequest(fakeUsername, fakePassword));
    }

    public void testSignInWithOhmage_noPassword_doesNotPerformNetworkRequest() {
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.sign_in_ohmage_button)).perform(click());

        verify(fakeRequestQueue, never()).add(new AccessTokenRequest(fakeUsername, fakePassword));
    }

    public void testClicking_createAccountButton_showsSignInFragment() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.sign_in_ohmage_frame)).check(matches(isDisplayed()));
    }

    public void testCreateAccount_validInputs_startsCreateAccountRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testCreateAccount_invalidEmail_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText("blah"));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue, never()).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testCreateAccount_noEmail_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue, never()).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testCreateAccount_noPassword_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue, never()).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testCreateAccount_noUsername_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue, never()).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testCreateAccount_noFullname_startsCreateAccountRequest() {
        onView(withId(R.id.create_account_button)).perform(click());
        User fakeUser = new User();
        fakeUser.fullName = "";
        fakeUser.username = fakeUsername;
        fakeUser.email = fakeEmail;

        onView(withId(R.id.username)).perform(scrollTo(), typeText(fakeUsername));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeRequestQueue).add(
                new CreateUserRequest(AuthUtil.GrantType.CLIENT_CREDENTIALS, fakePassword, fakeUser));
    }

    public void testClicking_signInWithGoogleButton_startsConnectOnPlusClient() throws Exception {
        onView(withId(R.id.sign_in_google_button)).perform(click());

        verify(fakePlusClientFragment).signIn(anyInt());
    }

    public void testPlusClientConnects_successfully_triesToGetGoogleAuthToken() throws Exception {
        setPlusClientFragmentSuccessfullyConnects();

        onView(withId(R.id.sign_in_google_button)).perform(click());

        verify(fakeAuthHelper).googleAuthGetToken(fakeGoogleEmail);
    }

    public void testGoogleAuthToken_receivedSuccessfully_triesToAuthWithOhmage() throws Throwable {
        setPlusClientFragmentSuccessfullyConnects();
        when(fakeAuthHelper.googleAuthGetToken(fakeGoogleEmail)).thenReturn(fakeGoogleToken);

        onView(withId(R.id.sign_in_google_button)).perform(click());

        verify(fakeRequestQueue).add(
                new AccessTokenRequest(AuthUtil.GrantType.GOOGLE_OAUTH2, fakeGoogleToken));
    }

    public void testAuthTokenReceived_validAccessToken_createsOhmageAccount() throws Throwable {
        final AccessToken fakeAccessToken = new AccessToken("token", "refresh", "user");
        setPlusClientFragmentSuccessfullyConnects();
        when(fakeAuthHelper.googleAuthGetToken(fakeGoogleEmail)).thenReturn(fakeGoogleToken);
        when(fakeRequestQueue.add(new AccessTokenRequest(AuthUtil.GrantType.GOOGLE_OAUTH2, fakeGoogleToken)))
                .then(new DeliverVolleyResultToBus(fakeAccessToken));

        onView(withId(R.id.sign_in_google_button)).perform(click());

        Account account = new Account("user", AuthUtil.ACCOUNT_TYPE);
        verify(fakeAccountManager).addAccountExplicitly(account, "refresh", null);
        verify(fakeAccountManager).setAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, "token");
    }

    private void setPlusClientFragmentSuccessfullyConnects() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().onSignedIn(mock(PlusClient.class));
            }
        });

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                PlusClient fakePlusClient = mock(PlusClient.class);
                when(fakePlusClient.getAccountName()).thenReturn(fakeGoogleEmail);
                getActivity().onSignedIn(fakePlusClient);
                return null;
            }
        }).when(fakePlusClientFragment).signIn(anyInt());
    }

    @Override
    public List<Object> getModules() {
        return Arrays.<Object>asList(new PlusClientFragmentTestModule());
    }
}
