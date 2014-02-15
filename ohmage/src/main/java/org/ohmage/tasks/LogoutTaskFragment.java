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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
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

    /**
     * Future provided by removeAccount call
     */
    private AccountManagerFuture<Boolean> res;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // This dialog should not be cancelable since it will always be fast
        // and it would be difficult to replace the account once it has been removed
        setCancelable(false);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (res != null) return;

        Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        //TODO: handle more than one account correctly
        if (accounts.length > 0) {
            res = accountManager.removeAccount(accounts[0], null, null);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.logging_out));
        return dialog;
    }
}
