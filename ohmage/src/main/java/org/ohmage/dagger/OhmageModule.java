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

package org.ohmage.dagger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;

import org.ohmage.app.MainActivity;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageAuthenticator;
import org.ohmage.app.OhmageErrorHandler;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthHelper;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.AuthenticateFragment;
import org.ohmage.auth.Authenticator;
import org.ohmage.auth.CreateAccountFragment;
import org.ohmage.auth.SignInFragment;
import org.ohmage.streams.StreamContentProvider;
import org.ohmage.sync.StreamSyncAdapter;
import org.ohmage.tasks.LogoutTaskFragment;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

@Module(
        injects = {
                MainActivity.class,
                AuthenticateFragment.class,
                Authenticator.class,
                CreateAccountFragment.class,
                SignInFragment.class,
                LogoutTaskFragment.class,
                StreamContentProvider.class,
                StreamSyncAdapter.class,
                OhmageAuthenticator.class
        },
        complete = false,
        library = true
)
public class OhmageModule {

    @Provides @Singleton AccountManager provideAccountManager(@ForApplication Context context) {
        return AccountManager.get(context);
    }

    @Provides @Singleton AuthHelper provideAuthHelper(@ForApplication Context context) {
        return new AuthHelper(context);
    }

    @Provides @Singleton Gson provideGson() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        return gson;
    }

    @Provides OhmageService provideOhmageService(@ForApplication Context context, Gson gson) {
        // Create an HTTP client that uses a cache on the file system.
        OkHttpClient okHttpClient = new OkHttpClient();
        try {
            HttpResponseCache cache = new HttpResponseCache(context.getCacheDir(), 1024);
            okHttpClient.setResponseCache(cache);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Executor executor = Executors.newCachedThreadPool();

        // Add an authenticator to handle 401 error by updating the token
        okHttpClient.setAuthenticator(new OhmageAuthenticator());

        // Add the ohmage token to each outgoing request for the current user
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override public void intercept(RequestFacade request) {
                AccountManager accountManager = AccountManager.get(Ohmage.app());
                Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
                if (accounts.length != 0) {
                    String token =
                            accountManager.peekAuthToken(accounts[0], AuthUtil.AUTHTOKEN_TYPE);
                    if (token != null) {
                        request.addHeader("Authorization", "ohmage " + token);
                    }
                }
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setExecutors(executor, executor)
                .setConverter(new GsonConverter(gson))
                .setClient(new OkClient(okHttpClient))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setErrorHandler(new OhmageErrorHandler())
                .setServer(Ohmage.API_ROOT)
                .setRequestInterceptor(requestInterceptor)
                .build();

        return restAdapter.create(OhmageService.class);

    }
}
