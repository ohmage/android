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
import org.ohmage.streams.StreamContract;
import org.ohmage.tasks.LogoutTaskFragment;

import javax.inject.Inject;

public class MainActivity extends InjectedActionBarActivity
        implements AdapterView.OnItemClickListener,
        LogoutTaskFragment.LogoutCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check to see if the account still exists.
        Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            startActivity(new Intent(this, AuthenticatorActivity.class));
            finish();
            return;
        }

        getContentResolver().requestSync(accounts[0], StreamContract.CONTENT_AUTHORITY,
                new Bundle());
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
        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = new HomeFragment();

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.content_frame, fragment)
                       .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        if (position == 0)
            setTitle(R.string.app_name);
        else
            setTitle(mNavigationItems[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
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
    public void onLogoutFinished() {
        startActivity(new Intent(this, AuthenticatorActivity.class));
        finish();
    }
}
