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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * General Util functions
 */
public class Util {

    /**
     * Starts an activity only if an activity exists that can be started
     *
     * @param context
     * @param intent
     * @return true if the activity was started
     */
    public static boolean safeStartActivity(Context context, @Nullable Intent intent) {
        if (activityExists(context, intent)) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Check to see if an activity exists to handle the given intent
     *
     * @param context
     * @param intent
     * @return true if at least one activity exists
     */
    public static boolean activityExists(Context context, @Nullable Intent intent) {
        PackageManager pm = context.getPackageManager();
        if (pm != null && intent != null) {
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            return activities != null && !activities.isEmpty();
        }
        return false;
    }
}
