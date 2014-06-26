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

package org.ohmage.auth.oauth;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.app.Util;
import org.ohmage.auth.AuthUtil;
import org.ohmage.condition.InvalidArgumentException;
import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.dagger.InjectedFragment;
import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.models.OAuthClient;
import org.ohmage.models.Stream;
import org.ohmage.operators.ContentProviderSaver.ContentProviderSaverSubscriber;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.widget.CompatArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Func1;
import rx.observers.SafeSubscriber;

public class OAuthActivity extends InjectedActionBarActivity {

    private static final String TAG = OAuthActivity.class.getSimpleName();
    private View mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        mLoadingView = findViewById(R.id.loading);

        Uri data = getIntent().getData();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, OAuthFragment.getInstance(data)).commit();
        }
    }

    public void showLoadingView(boolean show) {
        mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * A fragment to handle the oauth ui and network calls
     */
    public static class OAuthFragment extends InjectedFragment
            implements Observer<Stream>, LoaderCallbacks<Cursor> {

        public static Fragment getInstance(Uri uri) {
            OAuthFragment fragment = new OAuthFragment();
            Bundle args = new Bundle();
            args.putParcelable("uri", uri);
            fragment.setArguments(args);
            return fragment;
        }

        @Inject OhmageService ohmageService;

        @Inject AccountManager am;

        private CompatArrayAdapter<ScopePicker> mStreamsAdapter;

        private ListView list;
        private TextView mTitle;
        private TextView mDescription;
        private View mContainer;

        private Subscription authSupscription;

        private ScopeMap mScopeMap;

        private String mClient_id;
        private HashSet<ScopePicker> mScopes;
        private String mRedirectUri;
        private String mState;
        public OAuthClient mOauthClient;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Uri data = getArguments().getParcelable("uri");

            if (data == null) {
                handleError(R.string.oauth_no_data);
                return;
            }

            mRedirectUri = data.getQueryParameter("redirect_uri");
            if (TextUtils.isEmpty(mRedirectUri)) {
                handleError(R.string.oauth_missing_redirect_uri);
                return;
            }

            mState = data.getQueryParameter("state");

            mClient_id = data.getQueryParameter("client_id");
            if (TextUtils.isEmpty(mClient_id)) {
                handleError(R.string.oauth_missing_client_id);
                return;
            }

            String scope = data.getQueryParameter("scope");
            if (TextUtils.isEmpty(scope)) {
                handleError(R.string.oauth_missing_scope);
                return;
            }

            String[] scopeStrings = scope.split(" ");
            mScopes = new HashSet<ScopePicker>(scopeStrings.length);
            for (String currScopeString : scopeStrings) {
                // Build the scope, which will make sure it conforms to our format.
                try {
                    mScopes.add(ScopePicker.from((new Scope.Builder(currScopeString)).build()));
                } catch (InvalidArgumentException e) {
                    handleError(R.string.oauth_invalid_scope);
                    return;
                }
            }

            if (mScopes.isEmpty()) {
                handleError(R.string.oauth_missing_scope);
                return;
            }

            mScopeMap = new ScopeMap();
            mScopeMap.addAll(mScopes);

            setRetainInstance(true);
        }

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // The activity could be finishing if there was an error while creating the fragment
            if (getActivity().isFinishing()) {
                return;
            }

            Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
            if (accounts.length == 0) {
                Toast.makeText(getActivity(), R.string.oauth_no_account, Toast.LENGTH_SHORT).show();
                getActivity().finish();
                return;
            }

            mStreamsAdapter = new CompatArrayAdapter<ScopePicker>(getActivity(), 0) {

                LayoutInflater mInflater = LayoutInflater.from(getContext());

                public View getView(int position, View convertView, ViewGroup parent) {
                    return createViewFromResource(position, convertView, parent,
                            android.R.layout.simple_list_item_multiple_choice);
                }

                private View createViewFromResource(int position, View convertView,
                        ViewGroup parent,
                        int resource) {
                    CheckedTextView view;

                    if (convertView == null) {
                        view = (CheckedTextView) mInflater.inflate(resource, parent, false);
                    } else {
                        view = (CheckedTextView) convertView;
                    }

                    assert view != null;

                    final ScopePicker item = getItem(position);
                    view.setText(item.getName() + ": " + item.getPermission());
                    view.setChecked(item.isChecked());

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            item.setChecked(!item.isChecked());
                            ((CheckedTextView) v).setChecked(item.isChecked());
                        }
                    });

                    return view;
                }
            };

            getLoaderManager().initLoader(0, null, this);

            AndroidObservable.bindFragment(this, ohmageService.OAuthClientInfo(mClient_id))
                    .subscribe(
                            new SafeSubscriber<OAuthClient>(new Subscriber<OAuthClient>() {
                                @Override public void onCompleted() {
                                }

                                @Override public void onError(Throwable e) {
                                    if (e instanceof RetrofitError) {
                                        if (((RetrofitError) e).isNetworkError()) {
                                            Toast.makeText(getActivity(), R.string.network_error,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        handleError(R.string.oauth_server_error_for_client_info);
                                    }
                                }

                                @Override public void onNext(OAuthClient o) {
                                    if (o.redirectUri == null ||
                                        !o.redirectUri.equals(mRedirectUri)) {
                                        handleError(R.string.oauth_invalid_redirect_url);
                                    }
                                    mOauthClient = o;

                                    updateView();
                                }
                            })
                    );
        }

        @Override public void onDestroy() {
            super.onDestroy();
            if (authSupscription != null) {
                authSupscription.unsubscribe();
            }
        }

        @Override public void onCompleted() {
            // After all the streams have been downloaded try to show the view
            updateView();
        }

        @Override public void onError(Throwable e) {
            Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
        }

        // Called once for each remote stream that is downloaded
        @Override public void onNext(Stream stream) {
            // Add the name to all the scopes that match for this stream
            for (ScopePicker s : mScopeMap.get(stream)) {
                s.setName(stream.name);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_oauth, container, false);

            mContainer = rootView.findViewById(R.id.container);
            mContainer.setVisibility(View.GONE);

            list = (ListView) rootView.findViewById(R.id.streams);

            mTitle = (TextView) rootView.findViewById(R.id.title);
            mDescription = (TextView) rootView.findViewById(R.id.description);

            updateView();

            Button okButton = (Button) rootView.findViewById(R.id.ok);
            okButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    StringBuilder userScope = new StringBuilder();
                    for (int i = 0; i < mStreamsAdapter.getCount(); i++) {
                        ScopePicker picker = mStreamsAdapter.getItem(i);
                        if (picker.isChecked()) {
                            if (userScope.length() != 0) {
                                userScope.append(" ");
                            }
                            userScope.append(picker.toString());
                        }
                    }

                    if (TextUtils.isEmpty(userScope)) {
                        Toast.makeText(getActivity(), R.string.oauth_no_scopes_error,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Observable<String> authCall = ohmageService.OAuthAuthorize(mClient_id,
                            userScope.toString(), mState)
                            .onErrorReturn(new OnRedirect<String>() {

                                @Override public String parseHeader(String url) {
                                    return Uri.parse(url).getQueryParameter("code");
                                }
                            }).flatMap(new Func1<String, Observable<String>>() {
                                @Override public Observable<String> call(String code) {
                                    return ohmageService.OAuthAuthorized(true, code);
                                }
                            }).onErrorReturn(new OnRedirect<String>() {
                                @Override public String parseHeader(String url) {
                                    return url;
                                }
                            });

                    AndroidObservable.bindFragment(OAuthFragment.this, authCall).subscribe(
                            new SafeSubscriber<String>(new Subscriber<String>() {
                                @Override public void onCompleted() {
                                }

                                @Override public void onError(Throwable e) {
                                    if (e instanceof RetrofitError) {
                                        if (((RetrofitError) e).isNetworkError()) {
                                            Toast.makeText(getActivity(), R.string.network_error,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        handleError(R.string.oauth_server_error_authorizing);
                                    }
                                }

                                @Override public void onNext(String redirect) {
                                    Intent intent =
                                            new Intent(Intent.ACTION_VIEW, Uri.parse(redirect));
                                    if (Util.activityExists(getActivity(), intent)) {
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(getActivity(), "Invalid redirect uri",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    getActivity().finish();
                                }
                            })
                    );
                }
            });

            Button cancelButton = (Button) rootView.findViewById(R.id.cancel);
            cancelButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    getActivity().finish();
                }
            });

            return rootView;
        }

        // Updates the view once all the data is available
        private void updateView() {
            if (mOauthClient != null && allScopesHaveAName()) {
                mTitle.setText(mOauthClient.name);
                mDescription.setText(mOauthClient.description);
                mContainer.setVisibility(View.VISIBLE);

                mStreamsAdapter.clear();
                mStreamsAdapter.addAll(mScopes);
                list.setAdapter(mStreamsAdapter);

                ((OAuthActivity) getActivity()).showLoadingView(false);
            }
        }

        // Checks to make sure all scopes have a name to display
        private boolean allScopesHaveAName() {
            for (ScopePicker s : mScopes) {
                if (s.getName() == null) {
                    return false;
                }
            }
            return true;
        }

        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            //TODO: handle surveys as well as streams
            SelectParamBuilder builder = new SelectParamBuilder();
            for (ScopePicker scope : mScopes) {
                SelectParamBuilder sub = new SelectParamBuilder();
                sub.start(Streams.STREAM_ID, scope.getSchemaId());
                if (scope.getSchemaVersion() != null) {
                    sub.and(Streams.STREAM_VERSION, scope.getSchemaVersion());
                }
                builder.orSubSelect(sub);
            }

            return new CursorLoader(getActivity(), Streams.CONTENT_URI, Streams.DEFAULT_PROJECTION,
                    builder.buildSelection(), builder.buildParams(), null);
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            // First go through the streams on the phone
            Set<ScopePicker> localSchemas = new HashSet<ScopePicker>(mScopes.size());
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                Stream stream = new Stream();
                new ContentProviderReader().read(stream, cursor);

                for (ScopePicker s : mScopeMap.get(stream)) {
                    s.setName(stream.name);
                    // Add the scopes that match this stream to the set of local schemas
                    localSchemas.add(s);
                }
            }

            // Remove all the local streams from the set of scopes to get remote streams
            Set<ScopePicker> remoteSchemas = new HashSet<ScopePicker>();
            remoteSchemas.addAll(mScopes);
            remoteSchemas.removeAll(localSchemas);

            // If there are no remote streams we are ready to show the view
            if (remoteSchemas.isEmpty()) {
                updateView();
            } else {
                // Something is not in the db so go to the network
                ArrayList<Observable<Stream>> streamCalls = new ArrayList<Observable<Stream>>();

                for (final ScopePicker scope : remoteSchemas) {
                    if (scope.getSchemaVersion() == null) {
                        // First find the newest version, then download that stream
                        streamCalls
                                .add(ohmageService.getStreamVersions(scope.getSchemaId()).flatMap(
                                        new Func1<Collection<Integer>, Observable<Stream>>() {
                                            @Override public Observable<Stream> call(
                                                    Collection<Integer> versions) {
                                                return ohmageService.getStream(scope.getSchemaId(),
                                                        Collections.max(versions));
                                            }
                                        }
                                ));
                    } else {
                        streamCalls.add(ohmageService
                                .getStream(scope.getSchemaId(), scope.getSchemaVersion()).single()
                                .cache());
                    }
                }

                Observable<Stream> allCalls = Observable.merge(streamCalls);
                authSupscription = AndroidObservable.bindFragment(this, allCalls).subscribe(this);

                // Might as well save the streams to the db
                allCalls.subscribe(
                        new SafeSubscriber<Stream>(new ContentProviderSaverSubscriber(true)));
            }
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }

        /**
         * Shows an error to the user, tries to send an error message to the redirect uri, and
         * finishes the activity
         *
         * @param msg
         */
        private void handleError(int msg) {
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            if (mRedirectUri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mRedirectUri).buildUpon().appendQueryParameter("state", mState)
                                .build()
                );
                if (Util.activityExists(getActivity(), intent)) {
                    startActivity(intent);
                }
            }
            getActivity().finish();
        }
    }

    /**
     * Extends the regular scope class with information about the name of the stream, and if the
     * user checked or unchecked that particular scope
     */
    public static class ScopePicker extends Scope {

        public static ScopePicker from(Scope scope) {
            return new ScopePicker(scope.getType(), scope.getSchemaId(), scope.getSchemaVersion(),
                    scope.getPermission());
        }

        /**
         * The name for display purposes
         */
        private String mName;

        private boolean isChecked = true;

        public ScopePicker(Type type, String schemaId, Long schemaVersion, Permission permission)
                throws InvalidArgumentException {
            super(type, schemaId, schemaVersion, permission);
        }

        public void setName(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean isChecked) {
            this.isChecked = isChecked;
        }
    }

    /**
     * A simple helper set that lets the caller look up all scopes that match id and version
     */
    public static class ScopeMap {

        HashMap<String, HashSet<ScopePicker>> mMap = new HashMap<String, HashSet<ScopePicker>>();

        public void add(ScopePicker scope) {
            String key = scope.getSchemaId();
            if (scope.getSchemaVersion() != null) {
                key += "/" + scope.getSchemaVersion();
            }

            HashSet<ScopePicker> set = mMap.get(key);
            if (set == null) {
                set = new HashSet<ScopePicker>();
                mMap.put(key, set);
            }
            set.add(scope);
        }

        public void addAll(Collection<ScopePicker> scopes) {
            for (ScopePicker scope : scopes) {
                add(scope);
            }
        }

        public Collection<ScopePicker> get(String schemaId, long schemaVersion) {
            HashSet<ScopePicker> s = mMap.get(schemaId + "/" + schemaVersion);
            if (s == null) {
                s = mMap.get(schemaId);
            }
            if (s == null) {
                s = new HashSet<ScopePicker>();
            }
            return s;
        }

        public Collection<ScopePicker> get(Stream stream) {
            return get(stream.schemaId, stream.schemaVersion);
        }
    }

    /**
     * On redirects, instead of throwing an error, return the redirect url
     *
     * @param <T>
     */
    public abstract static class OnRedirect<T> implements Func1<Throwable, T> {
        @Override public T call(Throwable throwable) {
            //TODO: test no code
            if (throwable instanceof RetrofitError &&
                ((RetrofitError) throwable).getResponse() != null &&
                ((RetrofitError) throwable).getResponse().getStatus() == 302) {
                Response res = ((RetrofitError) throwable).getResponse();
                if (res != null) {
                    for (Header h : res.getHeaders()) {
                        if ("Location".equals(h.getName())) {
                            return parseHeader(h.getValue());
                        }
                    }
                }
            }
            throw new RuntimeException(throwable);
        }

        public abstract T parseHeader(String url);
    }
}
