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

package org.ohmage.provider;

import android.net.Uri;
import android.test.AndroidTestCase;

import org.ohmage.provider.OhmageContract.Ohmlets;
import org.ohmage.provider.OhmageContract.Streams;
import org.ohmage.provider.OhmageContract.Surveys;

/**
 * Tests {@link org.ohmage.helper.SelectParamBuilder}
 */
public class OhmageContractTest extends AndroidTestCase {

    public static final String fakeId = "fakeId";
    public static final Long fakeVersion = new Long(1);

    public static final Uri fakeOhmletUri =
            Ohmlets.CONTENT_URI.buildUpon().appendPath(fakeId).build();

    public static final Uri fakeSurveyUri =
            Surveys.CONTENT_URI.buildUpon().appendPath(fakeId).build();
    public static final Uri fakeSurveyVersionUri =
            Surveys.CONTENT_URI.buildUpon().appendPath(fakeId).appendPath(fakeVersion.toString())
                    .build();

    public static final Uri fakeStreamUri =
            Streams.CONTENT_URI.buildUpon().appendPath(fakeId).build();
    public static final Uri fakeStreamVersionUri =
            Streams.CONTENT_URI.buildUpon().appendPath(fakeId).appendPath(fakeVersion.toString())
                    .build();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testOhmletgetUriForOhmletId_validId_returnsUri() {
        Uri uri = Ohmlets.getUriForOhmletId(fakeId);

        assertEquals(fakeOhmletUri, uri);
    }

    public void testSurveygetUriForSurveyId_validId_returnsUri() {
        Uri uri = Surveys.getUriForSurveyId(fakeId);

        assertEquals(fakeSurveyUri, uri);
    }

    public void testSurveygetUriForSurveyIdVersion_validId_returnsUri() {
        Uri uri = Surveys.getUriForSurveyIdVersion(fakeId, fakeVersion);

        assertEquals(fakeSurveyVersionUri, uri);
    }

    public void testSurveygetId_noId_returnsNull() {
        String id = Surveys.getId(Surveys.CONTENT_URI);

        assertNull(id);
    }

    public void testSurveygetId_hasId_returnsId() {
        String id = Surveys.getId(fakeSurveyUri);

        assertEquals(fakeId, id);
    }

    public void testSurveygetId_hasIdVersion_returnsId() {
        String id = Surveys.getId(fakeSurveyVersionUri);

        assertEquals(fakeId, id);
    }

    public void testSurveygetVersion_noIdOrVersion_returnsNull() {
        Long version = Surveys.getVersion(Surveys.CONTENT_URI);

        assertNull(version);
    }

    public void testSurveygetId_noVersion_returnsId() {
        Long version = Surveys.getVersion(fakeSurveyUri);

        assertNull(version);
    }

    public void testSurveygetVersion_hasIdVersion_returnsId() {
        Long version = Surveys.getVersion(fakeSurveyVersionUri);

        assertEquals(fakeVersion, version);
    }

    public void testStreamgetUriForStreamId_validId_returnsUri() {
        Uri uri = Streams.getUriForStreamId(fakeId);

        assertEquals(fakeStreamUri, uri);
    }

    public void testStreamgetUriForStreamIdVersion_validId_returnsUri() {
        Uri uri = Streams.getUriForStreamIdVersion(fakeId, fakeVersion);

        assertEquals(fakeStreamVersionUri, uri);
    }

    public void testStreamgetId_noId_returnsNull() {
        String id = Streams.getId(Streams.CONTENT_URI);

        assertNull(id);
    }

    public void testStreamgetId_hasId_returnsId() {
        String id = Streams.getId(fakeStreamUri);

        assertEquals(fakeId, id);
    }

    public void testStreamgetId_hasIdVersion_returnsId() {
        String id = Streams.getId(fakeStreamVersionUri);

        assertEquals(fakeId, id);
    }

    public void testStreamgetVersion_noIdOrVersion_returnsNull() {
        Long version = Streams.getVersion(Streams.CONTENT_URI);

        assertNull(version);
    }

    public void testStreamgetId_noVersion_returnsId() {
        Long version = Streams.getVersion(fakeStreamUri);

        assertNull(version);
    }

    public void testStreamgetVersion_hasIdVersion_returnsId() {
        Long version = Streams.getVersion(fakeStreamVersionUri);

        assertEquals(fakeVersion, version);
    }

}
