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

package org.ohmage.test;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Contains helper methods for testing
 */
public class OhmageTest {

    public static void assertActivityCreated(ActivityInstrumentationTestCase2 test,
                                             final Class<? extends Activity> activityClass)
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final Stage[] stages =
                new Stage[]{Stage.CREATED, Stage.PRE_ON_CREATE, Stage.RESTARTED, Stage.RESUMED};
        test.getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                for (Stage s : stages) {
                    Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(s);
                    for (Activity activity : activities) {
                        if (activity.getClass().equals(activityClass)) {
                            latch.countDown();
                            return;
                        }
                    }
                }
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            test.fail("Activity " + activityClass.getSimpleName() + " not started");
        }
    }
}