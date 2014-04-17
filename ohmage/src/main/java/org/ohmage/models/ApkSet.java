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

package org.ohmage.models;

import android.content.Context;
import android.support.annotation.NonNull;

import org.ohmage.models.Stream.RemoteApp;
import org.ohmage.prompts.Prompt;
import org.ohmage.prompts.RemotePrompt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set of applications which may or may not be installed. Applications are unique by packageName,
 * and a newer version of an app will replace older versions in the set.
 */
public class ApkSet extends ArrayList<RemoteApp> {

    public static ApkSet fromSurveys(@NonNull ArrayList<Survey> surveys) {
        ApkSet set = new ApkSet();
        for (Survey survey : surveys) {
            set.addAll(fromPrompts(survey.surveyItems));
        }
        return set;
    }

    public static ApkSet fromStreams(@NonNull ArrayList<Stream> streams) {
        ApkSet set = new ApkSet();
        for (Stream stream : streams) {
            set.add(stream.app);
        }
        return set;
    }

    public static ApkSet fromPrompts(@NonNull ArrayList<Prompt> prompts) {
        ApkSet set = new ApkSet();
        for (Prompt prompt : prompts) {
            if (prompt instanceof RemotePrompt && ((RemotePrompt) prompt).getApp() != null) {
                set.add(((RemotePrompt) prompt).getApp());
            }
        }
        return set;
    }

    public static ApkSet fromPromptsIgnoreSkippable(@NonNull ArrayList<Prompt> prompts) {
        ApkSet set = new ApkSet();
        for (Prompt prompt : prompts) {
            if (prompt instanceof RemotePrompt && !prompt.isSkippable() &&
                ((RemotePrompt) prompt).getApp() != null) {
                set.add(((RemotePrompt) prompt).getApp());
            }
        }
        return set;
    }

    /**
     * Only add the newest version of an app that is required
     *
     * @param other
     * @return true if the item was added, false if it was already there
     */
    @Override public boolean add(RemoteApp other) {
        if (contains(other)) {
            RemoteApp old = get(indexOf(other));
            if (old.getVersionCode() < other.getVersionCode()) {
                remove(old);
            } else {
                return false;
            }
        }
        return super.add(other);
    }

    @Override public boolean addAll(Collection<? extends RemoteApp> collection) {
        boolean result = false;
        Iterator<? extends RemoteApp> it = collection.iterator();
        while (it.hasNext()) {
            if (add(it.next())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Clear out all apps which are now installed
     *
     * @param context
     */
    public void clearInstalled(final Context context) {
        Set keep = new HashSet();
        for (RemoteApp remoteApp : this) {
            if (!remoteApp.isInstalled(context)) {
                keep.add(remoteApp);
            }
        }
        clear();
        addAll(keep);
    }
}
