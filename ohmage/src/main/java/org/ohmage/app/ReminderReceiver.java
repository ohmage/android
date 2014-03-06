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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ohmage.provider.OhmageContract.Surveys;
import org.ohmage.reminders.glue.TriggerFramework;
import org.ohmage.reminders.notif.Notifier;

/**
 * Created by cketcham on 3/5/14.
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "TriggerNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        String urn = intent.getStringExtra(Notifier.KEY_CAMPAIGN_URN);

        if (TriggerFramework.ACTION_TRIGGER_NOTIFICATION.equals(action)) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Surveys.getUriForSurveyId(urn))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
