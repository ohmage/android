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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import com.squareup.okhttp.OkAuthenticator;

import junit.framework.Assert;

import org.ohmage.auth.AuthUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

/**
 * Tries to add the ohmage token to a request.
 */
public class OhmageAuthenticator implements OkAuthenticator {

    @Inject AccountManager accountManager;

    public OhmageAuthenticator() {
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public Credential authenticate(Proxy proxy, URL url, List<Challenge> challenges)
            throws IOException {
        for (Challenge challenge : challenges) {
            if (challenge.getScheme().equals("ohmage")) {
                Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
                if (accounts.length != 0) {
                    String oldToken = accountManager.peekAuthToken(accounts[0],
                            AuthUtil.AUTHTOKEN_TYPE);
                    if (oldToken != null) {
                        accountManager.invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, oldToken);

                        try {
                            String token = accountManager.blockingGetAuthToken(accounts[0],
                                    AuthUtil.AUTHTOKEN_TYPE, false);

                            // We can't retry the stream upload request automatically
                            if (url.getPath().startsWith("/ohmage/streams")) {
                                return null;
                            } else if (token != null) {
                                return ohmageToken(token);
                            }
                        } catch (OperationCanceledException e) {
                            e.printStackTrace();
                        } catch (AuthenticatorException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    private Credential ohmageToken(String token) {
        try {
            // TODO: when there is support for different types of Credentials, stop using reflection
            Constructor<?> constructor = Credential.class.getDeclaredConstructor(String.class);
            Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            return (Credential) constructor.newInstance("ohmage " + token);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Credential authenticateProxy(Proxy proxy, URL
            url, List<Challenge> challenges) throws IOException {
        return null;
    }
}