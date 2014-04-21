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
import android.accounts.OnAccountsUpdateListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.AuthenticatorActivity;
import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.fragments.HomeFragment;
import org.ohmage.fragments.OhmletsFragment;
import org.ohmage.fragments.StreamsFragment;
import org.ohmage.fragments.SurveysFragment;
import org.ohmage.reminders.ui.TriggerListActivity;
import org.ohmage.tasks.LogoutTaskFragment;

import javax.inject.Inject;

public class MainActivity extends InjectedActionBarActivity
        implements AdapterView.OnItemClickListener,
        OnAccountsUpdateListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * If this action is set, the streams fragment will be shown
     */
    public static final String EXTRA_VIEW_STREAMS = "extra_view_streams";

    @Inject AccountManager accountManager;

    /**
     * The sliding drawer
     */
    private DrawerLayout mDrawerLayout;

    /**
     * A list of navigation items which are shown in the sliding drawer
     */
    private String[] mNavigationItems;

    /**
     * The list which holds the navigation items
     */
    private ListView mDrawerList;

    /**
     * The drawer toggle button on the actionbar
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * An array which holds the icons for the navigation items
     */
    private TypedArray mNavigationIcons;

    /**
     * Set when the user wants to logout
     */
    private boolean mLoggingOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mNavigationItems = getResources().getStringArray(R.array.navigation_items);
        mNavigationIcons = getResources().obtainTypedArray(R.array.navigation_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.simple_list_item_activated_1, mNavigationItems) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Set the left drawable of the text view to be the icon for that item
                TextView view = (TextView) super.getView(position, convertView, parent);
                Drawable d = getResources()
                        .getDrawable(mNavigationIcons.getResourceId(position, -1));

                if (d != null && view != null) {
                    int bounds = getResources()
                            .getDimensionPixelSize(R.dimen.navigation_icon_bounds);
                    d.setBounds(0, 0, bounds, bounds);
                    view.setCompoundDrawables(d, null, null, null);
                }
                return view;
            }
        };

        mDrawerList.setAdapter(adapter);

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if(getIntent().getBooleanExtra(EXTRA_VIEW_STREAMS, false)) {
            setFragment(getString(R.string.streams));
        } else {
            setFragment(0);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getBooleanExtra(EXTRA_VIEW_STREAMS, false)) {
            String streams = getString(R.string.streams);
            for (int i = 0; i < mNavigationItems.length; i++) {
                if(streams.equals(mNavigationItems[i])) {
                    setFragment(i);
                    break;
                }
            }
        }
    }

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Watch to make sure the account still exists.
        accountManager.addOnAccountsUpdatedListener(this, null, true);
    }

    @Override protected void onPause() {
        super.onPause();

        accountManager.removeOnAccountsUpdatedListener(this);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (getString(R.string.reminders).equals(mNavigationItems[position])) {
            Intent intent = new Intent(this, TriggerListActivity.class);
            startActivity(intent);
        } else {
            setFragment(position);
        }
    }

    private void setFragment(int position) {
        // Set the fragment by the name
        setFragment(mNavigationItems[position]);

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        if (position == 0) {
            setTitle(R.string.app_name);
        } else {
            setTitle(mNavigationItems[position]);
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    public void setFragment(String id) {
        // Find the correct fragment to show
        Fragment fragment = new HomeFragment();
        if (getString(R.string.ohmlets).equals(id)) {
            fragment = new OhmletsFragment();
        } else if (getString(R.string.surveys).equals(id)) {
            fragment = new SurveysFragment();
        } else if (getString(R.string.streams).equals(id)) {
            fragment = new StreamsFragment();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO: show settings
                return true;

            case R.id.action_sign_out:
                mLoggingOut = true;
                FragmentManager fm = getSupportFragmentManager();
                LogoutTaskFragment logoutTaskFragment =
                        (LogoutTaskFragment) fm.findFragmentByTag("logout");

                // If the Fragment is non-null, then it is currently being
                // retained across a configuration change.
                if (logoutTaskFragment == null) {
                    logoutTaskFragment = new LogoutTaskFragment();
                    fm.beginTransaction().add(logoutTaskFragment, "logout").commit();
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        // This is a workaround for this bug
        // https://code.google.com/p/android/issues/detail?id=40323

        // If the fragment exists and has some back-stack entry
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment != null && fragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
            // Get the fragment fragment manager - and pop the back stack
            fragment.getChildFragmentManager().popBackStack();
        } else {
            // otherwise let super handle the back press
            super.onBackPressed();
        }
    }

    @Override public void onAccountsUpdated(Account[] accounts) {
        for (Account account : accounts) {
            if (AuthUtil.ACCOUNT_TYPE.equals(account.type)) {
                return;
            }
        }

        // No ohmage accounts so start the authenticator activity
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        if (mLoggingOut)
            intent.putExtra(AuthenticatorActivity.EXTRA_CLEAR_DEFAULT_ACCOUNT, true);
        startActivity(intent);
        finish();
    }
}
