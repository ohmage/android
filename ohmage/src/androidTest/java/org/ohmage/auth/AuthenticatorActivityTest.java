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

import com.google.android.gms.plus.PlusClient;

import org.apache.http.auth.AuthenticationException;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmageService.CancelableCallback;
import org.ohmage.app.R;
import org.ohmage.auth.AuthUtil.GrantType;
import org.ohmage.models.AccessToken;
import org.ohmage.models.User;
import org.ohmage.test.dagger.InjectedActivityInstrumentationTestCase;
import org.ohmage.test.dagger.PlusClientFragmentTestModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.withDecorView;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isClickable;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ohmage.test.OhmageViewMatchers.hasError;

/**
 * Functional Tests for the {@link org.ohmage.auth.AuthenticatorActivity}
 */
@LargeTest
public class AuthenticatorActivityTest
        extends InjectedActivityInstrumentationTestCase<AuthenticatorActivity> {

    @Inject AuthHelper fakeAuthHelper;
    @Inject AccountManager fakeAccountManager;
    @Inject OhmageService fakeOhmageService;
    @Inject PlusClientFragment fakePlusClientFragment;

    private static final String fakeGoogleEmail = "fake@gmail.com";
    private static final String fakeGoogleToken = "google_token";
    private static final String fakeEmail = "fake@email.com";
    private static final String fakePassword = "password";
    private static final String fakeFullname = "Full Name";
    private static final User fakeUser = new User();

    {
        fakeUser.email = fakeEmail;
        fakeUser.fullName = fakeFullname;
    }

    private CancelableCallback<User> fakeCallback = new CancelableCallback<User>() {
        @Override public void success(User user, Response response) {

        }

        @Override public void failure(RetrofitError error) {

        }
    };

    public AuthenticatorActivityTest() {
        super(AuthenticatorActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(fakeAccountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE))
                .thenReturn(new Account[]{});
        getActivity();
    }

    public void testStartingUp_noAccountExists_authButtonsAreVisible() {
        onView(withId(R.id.sign_in_google_button))
                .check(matches(isDisplayed()));

        onView(withId(R.id.sign_in_email_button))
                .check(matches(isDisplayed())).check(matches(isClickable()));

        onView(withId(R.id.create_account_button))
                .check(matches(isDisplayed())).check(matches(isClickable()));
    }

    public void testClicking_signInOhmageButton_showsSignInFragment() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.sign_in_ohmage_frame)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_validAccount_getsAccessToken() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        verify(fakeOhmageService).getAccessToken(eq(fakeEmail), eq(fakePassword),
                any(CancelableCallback.class));
    }

    public void testSignInWithOhmage_invalidAccount_showsSignInWithOhmage() throws Exception {
        when(fakeOhmageService.getAccessToken(fakeEmail, fakePassword))
                .thenThrow(new AuthenticationException());
        final Response fakeResponse = new Response(401, "", new ArrayList<Header>() {
        }, null);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ((CancelableCallback) invocation.getArguments()[2]).failure(
                        RetrofitError.httpError("", fakeResponse, null, null));
                return null;
            }
        }).when(fakeOhmageService).getAccessToken(eq(fakeEmail), eq(fakePassword),
                any(CancelableCallback.class));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_email_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_serverError_showsSignInWithOhmage() throws Exception {
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ((CancelableCallback) invocation.getArguments()[2]).failure(RetrofitError
                        .unexpectedError("url", new IOException()));
                return null;
            }
        }).when(fakeOhmageService).getAccessToken(eq(fakeEmail), eq(fakePassword),
                any(CancelableCallback.class));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_email_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noNetwork_showsSignInWithOhmage() throws Exception {
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ((CancelableCallback) invocation.getArguments()[2]).failure(RetrofitError
                        .networkError("url", new IOException()));
                return null;
            }
        }).when(fakeOhmageService).getAccessToken(eq(fakeEmail), eq(fakePassword),
                any(CancelableCallback.class));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_email_button)).check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noEmail_doesNotPerformNetworkRequest() throws Exception {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        verify(fakeOhmageService, never()).getAccessToken(anyString(), anyString());
    }

    public void testSignInWithOhmage_noEmail_showsErrorMessage() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withText(R.string.error_field_required))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noEmail_emailFieldHasError() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).check(matches(hasError(R.string.error_field_required)));
    }

    public void testSignInWithOhmage_noPassword_doesNotPerformNetworkRequest() throws Exception {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        verify(fakeOhmageService, never()).getAccessToken(anyString(), anyString());
    }

    public void testSignInWithOhmage_noPassword_showsErrorMessage() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withText(R.string.error_field_required))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    public void testSignInWithOhmage_noPassword_passwordFieldHasError() {
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.sign_in_email_button)).perform(click());

        onView(withId(R.id.password)).check(matches(hasError(R.string.error_field_required)));
    }

    public void testClicking_createAccountButton_showsSignInFragment() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.sign_in_ohmage_frame)).check(matches(isDisplayed()));
    }

    public void testCreateAccount_validInputs_startsCreateAccountRequestForUser() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeOhmageService).createUser(eq(fakePassword), argThat(new ArgumentMatcher<User>() {
            @Override public boolean matches(Object argument) {
                User arg = (User) argument;
                return fakeUser.email.equals(arg.email) && fakeUser.fullName.equals(arg.fullName);
            }
        }), any(CancelableCallback.class));
    }

    public void testCreateAccount_invalidEmail_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText("blah"));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeOhmageService, never())
                .createUser(any(GrantType.class), anyString(), any(User.class),
                        any(CancelableCallback.class));
    }

    public void testCreateAccount_invalidEmail_showsErrorMessage() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText("blah"));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withText(R.string.error_invalid_email_address))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    public void testCreateAccount_invalidEmail_emailFieldHasError() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText("blah"));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withId(R.id.email)).check(matches(hasError(R.string.error_invalid_email_address)));
    }

    public void testCreateAccount_noEmail_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeOhmageService, never())
                .createUser(any(GrantType.class), anyString(), any(User.class),
                        any(CancelableCallback.class));
    }

    public void testCreateAccount_noEmail_showsErrorMessage() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withText(R.string.error_field_required))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    public void testCreateAccount_noEmail_emailFieldHasError() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withId(R.id.email)).check(matches(hasError(R.string.error_field_required)));
    }

    public void testCreateAccount_noPassword_doesNotPerformNetworkRequest() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeOhmageService, never())
                .createUser(anyString(), any(User.class), any(CancelableCallback.class));
    }

    public void testCreateAccount_noPassword_showsErrorMessage() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withText(R.string.error_field_required))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    public void testCreateAccount_noPassword_passwordFieldHasError() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.fullname)).perform(scrollTo(), typeText(fakeFullname));
        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        onView(withId(R.id.password)).check(matches(hasError(R.string.error_field_required)));
    }

    public void testCreateAccount_noFullName_startsCreateAccountRequest() {
        onView(withId(R.id.create_account_button)).perform(click());
        final User fakeUser = new User();
        fakeUser.email = fakeEmail;

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        verify(fakeOhmageService).createUser(eq(fakePassword), argThat(new ArgumentMatcher<User>() {
            @Override public boolean matches(Object argument) {
                User arg = (User) argument;
                // Check that email and full name are the same as our fake user
                return fakeUser.email.equals(arg.email) &&
                       (fakeUser.fullName == null ? arg.fullName == null :
                               fakeUser.fullName.equals(arg.fullName));
            }
        }), any(CancelableCallback.class));
    }

    public void testCreateAccount_noFullName_fullNameFieldDoesNotHaveError() {
        onView(withId(R.id.create_account_button)).perform(click());

        onView(withId(R.id.email)).perform(scrollTo(), typeText(fakeEmail));
        onView(withId(R.id.password)).perform(scrollTo(), typeText(fakePassword));
        onView(withId(R.id.create_account_button)).perform(scrollTo(), click());

        // The view disappears once the create_account_button is clicked if there are no errors
        onView(withId(R.id.fullname)).check(doesNotExist());
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

        verify(fakeOhmageService).getAccessToken(eq(AuthUtil.GrantType.GOOGLE_OAUTH2),
                eq(fakeGoogleToken), any(CancelableCallback.class));
    }

    public void testAuthTokenReceived_validAccessToken_createsOhmageAccount() throws Throwable {
        final AccessToken fakeAccessToken = new AccessToken("token", "refresh", "user");
        setPlusClientFragmentSuccessfullyConnects();
        when(fakeAuthHelper.googleAuthGetToken(fakeGoogleEmail)).thenReturn(fakeGoogleToken);
        // For some reason the argument captor messes up the next test so I'm doing this
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ((CancelableCallback) invocation.getArguments()[2]).success(fakeAccessToken, null);
                return null;
            }
        }).when(fakeOhmageService).getAccessToken(eq(AuthUtil.GrantType.GOOGLE_OAUTH2),
                eq(fakeGoogleToken), any(CancelableCallback.class));

        onView(withId(R.id.sign_in_google_button)).perform(click());

        Account account = new Account(fakeGoogleEmail, AuthUtil.ACCOUNT_TYPE);
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
