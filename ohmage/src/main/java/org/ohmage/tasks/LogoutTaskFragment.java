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

package org.ohmage.tasks;

/**
 * Created by cketcham on 10/11/13.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import org.ohmage.app.R;
import org.ohmage.auth.AuthUtil;
import org.ohmage.dagger.InjectedDialogFragment;

import javax.inject.Inject;

/**
 * This Fragment shows a dialog as it removes the account from the system
 */
public class LogoutTaskFragment extends InjectedDialogFragment {

    @Inject AccountManager accountManager;

    private LogoutCallbacks mCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (LogoutCallbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // This dialog should not be cancelable since it will always be fast
        // and it would be difficult to replace the account once it has been removed
        setCancelable(false);

        Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        //TODO: handle more than one account correctly
        if (accounts.length > 0) {
            accountManager.removeAccount(accounts[0], new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    if (mCallbacks != null) {
                        mCallbacks.onLogoutFinished();
                    }
                }
            }, null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.logging_out));
        return dialog;
    }

    /**
     * This fragment only reports when the account has been removed
     */
    public static interface LogoutCallbacks {
        void onLogoutFinished();
    }
}