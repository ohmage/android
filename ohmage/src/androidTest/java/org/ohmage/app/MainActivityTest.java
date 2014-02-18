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

package org.ohmage.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.OnAccountsUpdateListener;
import android.os.Handler;
import android.test.suitebuilder.annotation.LargeTest;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.AuthenticatorActivity;
import org.ohmage.test.dagger.InjectedActivityInstrumentationTestCase;

import javax.inject.Inject;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerActions.closeDrawer;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerActions.openDrawer;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerMatchers.isClosed;
import static com.google.android.apps.common.testing.ui.espresso.contrib.DrawerMatchers.isOpen;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ohmage.test.OhmageTest.assertActivityCreated;

/**
 * Tests the {@link org.ohmage.app.MainActivity}
 */
@LargeTest
public class MainActivityTest extends InjectedActivityInstrumentationTestCase<MainActivity> {

    @Inject AccountManager fakeAccountManager;

    Account fakeAccount = new Account("user@blah.com", AuthUtil.ACCOUNT_TYPE);

    private String[] navigationItems;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        navigationItems = Ohmage.app().getResources().getStringArray(R.array.navigation_items);
    }

    public void testDrawer_initialState_DrawerIsClosed() {
        startActivityWithAccount();

        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

    public void testDrawer_clickingTheOhmageIcon_DrawerOpens() {
        startActivityWithAccount();

        onView(withId(android.R.id.home)).perform(click());

        onView(withId(R.id.drawer_layout)).check(matches(isOpen()));
    }

    public void testDrawer_openAndClose_opensAndClosesDrawer() {
        startActivityWithAccount();

        openDrawer(R.id.drawer_layout);

        // The drawer should now be open.
        onView(withId(R.id.drawer_layout)).check(matches(isOpen()));

        closeDrawer(R.id.drawer_layout);

        // Drawer should be closed again.
        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

    public void testDrawer_openAndClickItem_closesDrawer() {
        startActivityWithAccount();
        openDrawer(R.id.drawer_layout);
        int rowIndex = 2;
        String rowContents = navigationItems[rowIndex];

        onData(allOf(is(instanceOf(String.class)), is(rowContents))).perform(click());

        onView(withId(R.id.drawer_layout)).check(matches(isClosed()));
    }

    public void testStartActivity_accountDoesNotExist_RedirectsToAuthenticatorActivity()
            throws Exception {
        startActivityWithoutAccount();

        assertActivityCreated(this, AuthenticatorActivity.class);
    }

    public void testMenu_clickingSignOut_removesAccount() throws Exception {
        startActivityWithAccount();

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_sign_out)).perform(click());

        verify(fakeAccountManager).removeAccount(
                eq(fakeAccount), any(AccountManagerCallback.class), any(Handler.class));
    }

    public void testMenu_clickingSignOut_RedirectsToAuthenticatorActivity() throws Exception {
        startActivityWithAccount();
        // Call the callback to simulate account removal finished
        when(fakeAccountManager.removeAccount(eq(fakeAccount), any(AccountManagerCallback.class),
                any(Handler.class))).then(new Answer<Object>() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                getActivity().onAccountsUpdated(new Account[]{});
                return null;
            }
        });

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_sign_out)).perform(click());

        assertActivityCreated(this, AuthenticatorActivity.class);
    }

    private void startActivityWithAccount() {
        startActivityWithAccounts(new Account[]{fakeAccount});
    }

    private void startActivityWithoutAccount() {
        startActivityWithAccounts(new Account[]{});
    }

    private void startActivityWithAccounts(final Account[] accounts) {
        when(fakeAccountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE)).thenReturn(accounts);

        doAnswer(new Answer<Object>() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ((OnAccountsUpdateListener) invocation.getArguments()[0]).onAccountsUpdated(
                        accounts);
                return null;
            }
        }).when(fakeAccountManager).addOnAccountsUpdatedListener(
                any(OnAccountsUpdateListener.class), any(Handler.class), eq(true));
        getActivity();
    }
}

