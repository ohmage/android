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

package org.ohmage.auth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;

import org.ohmage.app.R;

public class AccountNotActivatedDialog extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            new AccountNotActivatedFragment().show(getSupportFragmentManager(), "dialog");
        }
    }

    public static class AccountNotActivatedFragment extends DialogFragment {

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_account_not_activated)
                    .setMessage(R.string.message_account_not_activated)
                    .setPositiveButton(R.string.ok, null);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            super.onDismiss(dialogInterface);
            getActivity().finish();
        }
    }

}
