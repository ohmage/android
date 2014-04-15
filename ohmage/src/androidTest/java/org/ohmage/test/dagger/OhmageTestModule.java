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

package org.ohmage.test.dagger;

import android.accounts.AccountManager;
import android.content.Context;

import com.google.gson.Gson;

import org.ohmage.app.InstallSurveyDependencies;
import org.ohmage.app.InstallSurveyDependencies.InstallSurveyDependenciesFragment;
import org.ohmage.app.MainActivity;
import org.ohmage.app.MainActivityOhmletsTest;
import org.ohmage.app.MainActivityTest;
import org.ohmage.app.OhmageAuthenticator;
import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmletActivity;
import org.ohmage.app.SurveyActivity;
import org.ohmage.app.SurveyActivity.SurveyPromptLoader;
import org.ohmage.auth.AuthHelper;
import org.ohmage.auth.AuthenticateFragment;
import org.ohmage.auth.Authenticator;
import org.ohmage.auth.AuthenticatorTest;
import org.ohmage.auth.CreateAccountFragment;
import org.ohmage.auth.SignInFragment;
import org.ohmage.dagger.ForApplication;
import org.ohmage.fragments.HomeFragment;
import org.ohmage.fragments.InstallDependenciesDialog;
import org.ohmage.fragments.OhmletsFragment;
import org.ohmage.fragments.OhmletsGridFragment;
import org.ohmage.fragments.OhmletsSearchFragment;
import org.ohmage.fragments.StreamsFragment;
import org.ohmage.fragments.SurveysFragment;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.prompts.PromptFragment;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.StreamContentProvider;
import org.ohmage.sync.OhmageSyncAdapter;
import org.ohmage.sync.ResponseSyncAdapter;
import org.ohmage.sync.StreamSyncAdapter;
import org.ohmage.sync.StreamSyncAdapterTest;
import org.ohmage.tasks.LogoutTaskFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module(
        injects = {
                InjectedAndroidTestCase.class,
                MainActivityTest.class,
                AuthenticatorTest.class,
                StreamSyncAdapterTest.class,
                MainActivityOhmletsTest.class,

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
        },
        complete = false,
        library = true,
        overrides = true
)
public class OhmageTestModule {

    @Provides @Singleton AccountManager provideAccountManager(@ForApplication Context context) {
        return mock(AccountManager.class);
    }

    @Provides @Singleton AuthHelper provideAuthHelper() {
        return mock(AuthHelper.class);
    }

    @Provides @Singleton Gson provideGson() {
//        Gson gson = new GsonBuilder()
//                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//                .create();
        return mock(Gson.class);
    }

    @Provides @Singleton OhmageService provideOhmageService() {
        return mock(OhmageService.class);
    }
}
