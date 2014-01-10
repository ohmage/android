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

import android.test.ActivityInstrumentationTestCase2;

import org.ohmage.app.Ohmage;
import org.ohmage.dagger.AndroidModule;
import org.ohmage.dagger.InjectedActionBarActivity;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Overrides the main application graph for testing, and injects mock objects into the test
 */
public class InjectedActivityInstrumentationTestCase<T extends InjectedActionBarActivity> extends
        ActivityInstrumentationTestCase2<T> {

    protected ObjectGraph graph;

    public InjectedActivityInstrumentationTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        graph = ObjectGraph.create(new AndroidModule(Ohmage.app()),
                new OhmageTestModule()).plus(getModules().toArray());
        Ohmage.app().setApplicationGraph(graph);
        graph.inject(this);
    }

    protected List<Object> getModules() {
        return Arrays.<Object>asList();
    }

    @Override
    protected void tearDown() throws Exception {
        graph = null;
        super.tearDown();
    }
}
