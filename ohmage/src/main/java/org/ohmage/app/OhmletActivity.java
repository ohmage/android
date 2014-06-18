/*
 * Copyright (C) 2014 ohmage
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.Authenticator;
import org.ohmage.auth.AuthenticatorActivity;
import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.dagger.InjectedDialogFragment;
import org.ohmage.dagger.InjectedFragment;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Ohmlet.Role;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.OhmageContract.Ohmlets;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.schedulers.Schedulers;
import rx.util.functions.Action1;

public class OhmletActivity extends InjectedActionBarActivity {

    private static final String TAG = OhmletActivity.class.getSimpleName();
    private View mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        mLoadingView = findViewById(R.id.loading);

        String action = getIntent().getAction();
        Bundle extras = new Bundle();
        Uri data = getIntent().getData();
        if (data.getScheme().equals("https") || data.getScheme().equals("http")) {
            if (data.getPathSegments().size() > 3) {
                if (data.getPathSegments().get(3).equals("invitation")) {
                    action = OhmletFragment.ACTION_JOIN;
                }
            }
            extras.putString(OhmletFragment.EXTRA_OHMLET_INVITATION_ID,
                    data.getQueryParameter("ohmlet_invitation_id"));
            extras.putString(AuthenticatorActivity.EXTRA_USER_INVITATION_CODE,
                    data.getQueryParameter("user_invitation_id"));
            extras.putString(AuthenticatorActivity.EXTRA_EMAIL, data.getQueryParameter("email"));
            data = Ohmlets.getUriForOhmletId(data.getPathSegments().get(2));
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    OhmletFragment.getInstance(action, data, extras)).commit();
        }
    }

    public void showLoadingView(boolean show) {
        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ohmlet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class OhmletFragment extends InjectedFragment
            implements Observer<Ohmlet>, OnClickListener, LoaderCallbacks<Cursor> {

        public static final String EXTRA_OHMLET_INVITATION_ID = "extra_ohmlet_invitation_id";

        /**
         * Join an ohmlet
         */
        public static final String ACTION_JOIN = "org.ohmage.action.JOIN";

        public static Fragment getInstance(String action, Uri uri, Bundle extras) {
            OhmletFragment fragment = new OhmletFragment();
            extras.putString("action", action);
            extras.putParcelable("uri", uri);
            fragment.setArguments(extras);
            return fragment;
        }

        @Inject OhmageService ohmageService;

        @Inject AccountManager am;

        private TextView mDescription;

        private Ohmlet mOhmlet;

        private Subscription ohmletSupscription;

        private Button mJoinButton;

        private String userId;

        private Uri uri;

        private String ohmletId;

        private String action;

        private String invitationCode;

        private JoinOhmletDialog mJoinDialog;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            action = getArguments().getString("action");
            uri = getArguments().getParcelable("uri");
            invitationCode = getArguments().getString(EXTRA_OHMLET_INVITATION_ID);
            ohmletId = uri.getLastPathSegment();
        }

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
            if (accounts.length == 0) {
                if (ACTION_JOIN.equals(action)) {
                    Intent intent = new Intent(getActivity(), AuthenticatorActivity.class);
                    Bundle extras = new Bundle(getArguments());
                    extras.remove("uri");
                    extras.remove("action");
                    intent.putExtras(extras);
                    intent.putExtra(AuthenticatorActivity.EXTRA_JOIN_OHMLET_ID, ohmletId);
                    startActivity(intent);
                }
                getActivity().finish();
                return;
            }

            userId = am.getUserData(accounts[0], Authenticator.USER_ID);

            if (savedInstanceState == null) {
                if (ACTION_JOIN.equals(action)) {
                    mJoinDialog = (JoinOhmletDialog) getChildFragmentManager().findFragmentByTag("join_dialog");
                }
            }

            getLoaderManager().initLoader(0, null, this);
        }

        @Override public void onDestroy() {
            super.onDestroy();
            if (ohmletSupscription != null)
                ohmletSupscription.unsubscribe();
        }

        @Override public void onCompleted() {
            updateView();
        }

        @Override public void onError(Throwable e) {
            //TODO: handle error correctly
            Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        @Override public void onNext(Ohmlet ohmlet) {
            setOhmlet(ohmlet);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_ohmlet, container, false);
            assert rootView != null;

            mDescription = (TextView) rootView.findViewById(R.id.description);
            mDescription.setVisibility(View.GONE);
            mJoinButton = (Button) rootView.findViewById(R.id.join);
            mJoinButton.setVisibility(View.GONE);
            mJoinButton.setOnClickListener(this);

            if (mOhmlet != null)
                updateView();

            return rootView;
        }

        public void setOhmlet(Ohmlet ohmlet) {
            mOhmlet = ohmlet;
        }

        public Ohmlet getOhmlet() {
            return mOhmlet;
        }

        public String getUserId() {
            return userId;
        }

        public String getInvitationCode() {
            return invitationCode;
        }

        private void updateView() {
            ((OhmletActivity) getActivity()).showLoadingView(false);
            getActivity().setTitle(mOhmlet.name);

            if (getView() != null) {
                mDescription.setVisibility(View.VISIBLE);
                mDescription.setText(mOhmlet.description);
                mJoinButton.setVisibility(mOhmlet.isMember(userId) ? View.GONE : View.VISIBLE);
            }
        }

        @Override public void onClick(View v) {
            JoinOhmletDialog.getInstance().show(getChildFragmentManager(), "join_dialog");
        }

        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(), Ohmlets.getUriForOhmletId(ohmletId),
                    Ohmlets.DEFAULT_PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                Ohmlet ohmlet = new Ohmlet();
                new ContentProviderReader().read(ohmlet, cursor);
                setOhmlet(ohmlet);
                updateView();
            } else {
                // It is not in the db so go to the network
                Observable<Ohmlet> ohmletObservable =
                        ohmageService.getOhmlet(ohmletId).single().cache();

                ohmletSupscription = AndroidObservable.fromFragment(this, ohmletObservable)
                        .subscribe(this);
            }
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {

        }
    }

    public static class JoinOhmletDialog extends InjectedDialogFragment
            implements DialogInterface.OnClickListener {

        public static JoinOhmletDialog getInstance() {
            return new JoinOhmletDialog();
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Join ohmlet?")
                    .setMessage("Are you sure you want to join this ohmlet?")
                    .setPositiveButton(R.string.join, this)
                    .setNegativeButton(R.string.cancel, null);
            return builder.create();
        }

        @Override public void onClick(DialogInterface dialog, int which) {

            OhmletFragment parent = ((OhmletFragment) getParentFragment());

            String userId = parent.getUserId();
            Ohmlet ohmlet = parent.getOhmlet();
            String id = TextUtils.isEmpty(userId) ? "me" : userId;

            if (!ohmlet.isMember(id)) {
                Observable.from(ohmlet).subscribeOn(Schedulers.io()).doOnNext(
                        new ContentProviderSaver()).doOnError(new Action1<Throwable>() {
                    @Override public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }).subscribe();

                Member m = new Member();
                m.memberId = id;
                m.role = Role.MEMBER;
                m.code = parent.getInvitationCode();
                ohmlet.people.add(m);
            }
        }
    }
}
