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

import android.test.AndroidTestCase;

import org.ohmage.models.Stream.RemoteApp;
import org.ohmage.models.Stream.RemoteApp.Android;
import org.ohmage.prompts.Prompt;

import java.util.ArrayList;

/**
 * Tests {@link ApkSet} to make sure it handles creating the correct set of items
 */
public class ApkSetTest extends AndroidTestCase {

    private static final String FAKE_PACKAGENAME_1 = "org.fake.package";
    private static final String FAKE_PACKAGENAME_2 = "org.fake.package2";
    private static final int FAKE_VERSION_1 = 1;
    private static final int FAKE_VERSION_2 = 2;

    private ApkSet set;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        set = new ApkSet();
    }

    public void testFromSurveys_noSurveys_emptySet() {
        set = ApkSet.fromSurveys(new ArrayList<Survey>());

        assertTrue(set.isEmpty());
    }

    public void testFromSurveys_oneSurveyNoPrompts_emptySet() {

    }

    public void testFromSurveys_oneSurveyHasPrompts_hasItems() {

    }

    public void testFromStreams_noStreams_emptySet() {
        set = ApkSet.fromStreams(new ArrayList<Stream>());

        assertTrue(set.isEmpty());
    }

    public void testFromStreams_oneStream_hasItem() {

    }

    public void testFromStreams_multipleStreams_hasItems() {

    }

    public void testFromPrompts_noPrompts_emptySet() {
        set = ApkSet.fromPrompts(new ArrayList<Prompt>());

        assertTrue(set.isEmpty());
    }

    public void testFromPrompts_onePrompt_hasItem() {

    }

    public void testFromPrompts_multiplePrompts_hasItems() {

    }

    public void testFromPromptsIgnoreSkippable_noPrompts_emptySet() {
        set = ApkSet.fromPromptsIgnoreSkippable(new ArrayList<Prompt>());

        assertTrue(set.isEmpty());
    }

    public void testFromPromptsIgnoreSkippable_onePrompt_hasItem() {

    }

    public void testFromPromptsIgnoreSkippable_multiplePrompts_hasItems() {

    }

    public void testAdd_otherPromptDoesNotExist_added() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_2, FAKE_VERSION_2));

        assertTrue(res);
        assertEquals(2, set.size());
    }

    public void testAdd_otherPromptDoesExistVersionLess_notAdded() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_2));

        assertEquals(1, set.size());
    }

    public void testAdd_otherPromptDoesExistVersionLess_replaced() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_2));

        assertTrue(res);
        assertEquals(set.get(0).getVersionCode(), FAKE_VERSION_2);
    }

    public void testAdd_otherPromptDoesExistVersionEqual_notReplaced() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        assertFalse(res);
    }

    public void testAdd_otherPromptDoesExistVersionEqual_notAdded() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        assertFalse(res);
        assertEquals(1, set.size());
    }

    public void testAdd_otherPromptDoesExistVersionGreater_notReplaced() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_2));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        assertFalse(res);
        assertEquals(set.get(0).getVersionCode(), FAKE_VERSION_2);
    }

    public void testAdd_otherPromptDoesExistVersionGreater_notAdded() {
        set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_2));

        boolean res = set.add(createApp(FAKE_PACKAGENAME_1, FAKE_VERSION_1));

        assertFalse(res);
        assertEquals(1, set.size());
    }

    private RemoteApp createApp(String packageName, int version) {
        RemoteApp app = new RemoteApp();
        app.android = new Android();
        app.android.packageName = packageName;
        app.android.versionCode = version;
        return app;
    }
}
