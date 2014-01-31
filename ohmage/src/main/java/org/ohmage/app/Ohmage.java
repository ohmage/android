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

package org.ohmage.app;

import android.app.Application;

import org.ohmage.dagger.AndroidModule;
import org.ohmage.dagger.OhmageModule;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Main ohmage application class
 */
public class Ohmage extends Application {

    /**
     * The protocol and hostname used to access the ohmage service.
     */
    public static final String API_HOST = "https://dev.ohmage.org";

    /**
     * The URL root used to access the ohmage API.
     */
    public static final String API_ROOT = API_HOST + "/ohmage";

    /**
     * Static reference to self
     */
    private static Ohmage self;

    /**
     * The object graph used by dagger for this application
     */
    private ObjectGraph applicationGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        self = this;
    }

    /**
     * A static reference to the application context
     *
     * @return the application context
     */
    public static Ohmage app() {
        return self;
    }

    /**
     * A list of modules to use for the application graph. Subclasses can override this method to
     * provide additional modules provided they call {@code super.getModules()}.
     */
    protected List<Object> getModules() {
        return Arrays.<Object>asList(new AndroidModule(this), new OhmageModule());
    }

    /**
     * The Object Graph that other objects should inject themselves into
     *
     * @return the object graph used by the application
     */
    public ObjectGraph getApplicationGraph() {
        if (applicationGraph == null)
            applicationGraph = ObjectGraph.create(getModules().toArray());
        return applicationGraph;
    }

    /**
     * Allow tests to set the application graph
     *
     * @param graph
     */
    public void setApplicationGraph(ObjectGraph graph) {
        applicationGraph = graph;
    }
}
