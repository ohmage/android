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
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;

import org.ohmage.app.InstallSurveyDependencies;
import org.ohmage.app.InstallSurveyDependencies.InstallSurveyDependenciesFragment;
import org.ohmage.app.MainActivity;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageAuthenticator;
import org.ohmage.app.OhmageErrorHandler;
import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmletActivity;
import org.ohmage.app.SurveyActivity;
import org.ohmage.app.SurveyActivity.SurveyPromptLoader;
import org.ohmage.auth.AuthHelper;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.AuthenticateFragment;
import org.ohmage.auth.Authenticator;
import org.ohmage.auth.CreateAccountFragment;
import org.ohmage.auth.SignInFragment;
import org.ohmage.fragments.HomeFragment;
import org.ohmage.fragments.InstallDependenciesDialog;
import org.ohmage.fragments.OhmletsFragment;
import org.ohmage.fragments.OhmletsGridFragment;
import org.ohmage.fragments.OhmletsSearchFragment;
import org.ohmage.fragments.StreamsFragment;
import org.ohmage.fragments.SurveysFragment;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.prompts.BasePrompt;
import org.ohmage.prompts.Prompt;
import org.ohmage.prompts.PromptFragment;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.StreamContentProvider;
import org.ohmage.sync.OhmageSyncAdapter;
import org.ohmage.sync.ResponseSyncAdapter;
import org.ohmage.sync.StreamSyncAdapter;
import org.ohmage.tasks.LogoutTaskFragment;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
                OhmageAuthenticator.class,
                OhmletsSearchFragment.class,
                OhmletsGridFragment.OhmletAdapter.class,
                OhmletsFragment.class,
                OhmageSyncAdapter.class,
                OhmletActivity.class,
                OhmletActivity.OhmletFragment.class,
                OhmletActivity.JoinOhmletDialog.class,
                ContentProviderSaver.class,
                ContentProviderReader.class,
                SurveysFragment.class,
                HomeFragment.class,
                SurveyActivity.class,
                PromptFragment.class,
                SurveyPromptLoader.class,
                ResponseSyncAdapter.class,
                StreamsFragment.class,
                InstallSurveyDependencies.class,
                InstallSurveyDependenciesFragment.class,
                InstallDependenciesDialog.class,
                SurveyActivity.class,
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
                .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
                .registerTypeAdapter(Prompt.class, new BasePrompt.PromptDeserializer())
                .registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                    @Override public JsonElement serialize(Double src, Type typeOfSrc,
                            JsonSerializationContext context) {
                        if ((src == Math.floor(src)) && !Double.isInfinite(src)) {
                            return context.serialize(src.intValue());
                        }

                        return context.serialize(src);
                    }
                })
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

    /**
     * Factory which translates strings from the server to enums
     */
    public static class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<T> rawType = (Class<T>) type.getRawType();
            if (!rawType.isEnum()) {
                return null;
            }

            final Map<String, T> lowercaseToConstant = new HashMap<String, T>();
            for (T constant : rawType.getEnumConstants()) {
                lowercaseToConstant.put(toLowercase(constant), constant);
            }

            return new TypeAdapter<T>() {
                public void write(JsonWriter out, T value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(toLowercase(value));
                    }
                }

                public T read(JsonReader reader) throws IOException {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        return null;
                    } else {
                        return lowercaseToConstant.get(reader.nextString());
                    }
                }
            };
        }

        private String toLowercase(Object o) {
            return o.toString().toLowerCase(Locale.US);
        }
    }
}
