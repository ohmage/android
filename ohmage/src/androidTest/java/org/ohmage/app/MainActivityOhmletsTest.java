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
import android.test.suitebuilder.annotation.LargeTest;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlets;
import org.ohmage.test.dagger.InjectedActivityInstrumentationTestCase;

import javax.inject.Inject;

import retrofit.Callback;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasFocus;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests specifically the ohmlets part of the {@link MainActivity}
 */
@LargeTest
public class MainActivityOhmletsTest extends InjectedActivityInstrumentationTestCase<MainActivity> {

    @Inject AccountManager fakeAccountManager;
    @Inject OhmageService fakeOhmageService;

    Account fakeAccount = new Account("username", AuthUtil.ACCOUNT_TYPE);

    public MainActivityOhmletsTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        when(fakeAccountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE))
                .thenReturn(new Account[]{fakeAccount});
        MainActivity activity = getActivity();

        // Open the ohmlets page
        activity.setFragment(activity.getString(R.string.ohmlets));
    }

    public void testClickingSearchIcon_initialState_showsSearchTextBox() {
        onView(withId(R.id.search_button)).perform(click());
        onView(withId(R.id.search_src_text))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()));
    }

    public void testTypingText_inSearchBox_sendsTheCorrectQueryToTheNetwork()
            throws InterruptedException {
        onView(withId(R.id.search_button)).perform(click());
        String query = "a";

        onView(withId(R.id.search_src_text)).perform(typeText(query)).perform();
        // Wait for a few milliseconds so the request will actually happen
        Thread.sleep(250);

        verify(fakeOhmageService).searchOhmlets(eq(query), any(Callback.class));
    }

    public void testReceivingList_withItems_showsItemsInList() {
        onView(withId(R.id.search_button)).perform(click());
        String query = "a";
        final Ohmlets ohmlets = new Ohmlets();
        addFakeOhmlet(ohmlets);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Callback<Ohmlets> cb = (Callback<Ohmlets>) invocationOnMock.getArguments()[1];
                cb.success(ohmlets, null);
                return null;
            }
        }).when(fakeOhmageService).searchOhmlets(eq(query), any(Callback.class));

        onView(withId(R.id.search_src_text)).perform(typeText(query));
    }
//
//    public void testTypingText_noPreviousQuery_showsSpinnerWhileRequestIsBeingMade() {
//
//    }
//
//    public void testTypingText_noDataForPreviousQuery_showsSpinnerWhileRequestIsBeingMade() {
//
//    }
//
//    public void testQuicklyTypingText_searchBoxIsSelected_onlySendsOneNetworkRequestAtTheEnd() {
//
//    }
//
//    public void testTypingText_afterPreviousQueryMadeWithReturnedData_continuesToShowOldData() {
//
//    }
//
//    public void testTypingText_afterPreviousQueryMadeWithReturnedData_showsNewItemsAfterTheRequestIsComplete() {
//
//    }

    private void addFakeOhmlet(Ohmlets ohmlets) {
        Ohmlet p = new Ohmlet();
        p.ohmletId = ohmlets.size() + 1 + "";
        p.name = "Blah Ohmlet";
        p.description = "This is a fake ohmlet to collect blah data";
        ohmlets.add(p);
    }
}

