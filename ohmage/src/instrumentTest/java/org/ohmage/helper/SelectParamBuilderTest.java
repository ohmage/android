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

package org.ohmage.helper;

import android.test.AndroidTestCase;

/**
 * Tests {@link SelectParamBuilder}
 */
public class SelectParamBuilderTest extends AndroidTestCase {

    private SelectParamBuilder mSelectParamBuilder;

    private String fakeKey = "fakeKey";

    private String fakeValue = "fakeValue";

    private String fakeKey2 = "fakeKey2";

    private String fakeValue2 = "fakeValue2";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mSelectParamBuilder = new SelectParamBuilder();
    }

    public void testStart_alreadyUsedBuilder_throwsException() {
        mSelectParamBuilder.and(fakeKey, fakeValue);

        try {
            mSelectParamBuilder.start(fakeKey, fakeValue);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
        }
    }

    public void testBuildSelection_emptySelection_returnsEmptyString() {
        assertEquals("", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_oneAnd_correctSelection() {
        mSelectParamBuilder.and(fakeKey, fakeValue);

        assertEquals(fakeKey + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_twoAnds_correctSelection() {
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? AND " + fakeKey2 + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_oneOr_correctSelection() {
        mSelectParamBuilder.or(fakeKey, fakeValue);

        assertEquals(fakeKey + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_twoOrs_correctSelection() {
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? OR " + fakeKey2 + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_AndAndOr_correctSelection() {
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? OR " + fakeKey2 + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_OrAndAnd_correctSelection() {
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? AND " + fakeKey2 + "=?", mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_AndAndAndAndOr_correctSelection() {
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? AND " + fakeKey + "=? OR " + fakeKey2 + "=?",
                mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_AndAndOrAndOr_correctSelection() {
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? OR " + fakeKey + "=? OR " + fakeKey2 + "=?",
                mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_OrAndOrAndAnd_correctSelection() {
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? OR " + fakeKey + "=? AND " + fakeKey2 + "=?",
                mSelectParamBuilder.buildSelection());
    }

    public void testBuildSelection_OrAndAndAndAnd_correctSelection() {
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey2, fakeValue2);

        assertEquals(fakeKey + "=? AND " + fakeKey + "=? AND " + fakeKey2 + "=?",
                mSelectParamBuilder.buildSelection());
    }

    public void testBuildParams_emptyParams_returnsEmptyArray() {

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(0, params.length);
    }

    public void testBuildParams_oneAnd_correctParams() {
        mSelectParamBuilder.and(fakeKey, fakeValue);

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(1, params.length);
        assertEquals(fakeValue, params[0]);
    }

    public void testBuildParams_twoAnds_correctParams() {
        mSelectParamBuilder.and(fakeKey, fakeValue);
        mSelectParamBuilder.and(fakeKey2, fakeValue2);

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(2, params.length);
        assertEquals(fakeValue, params[0]);
        assertEquals(fakeValue2, params[1]);
    }

    public void testBuildParams_oneOr_correctParams() {
        mSelectParamBuilder.or(fakeKey, fakeValue);

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(1, params.length);
        assertEquals(fakeValue, params[0]);
    }

    public void testBuildParams_twoOrs_correctParams() {
        mSelectParamBuilder.or(fakeKey, fakeValue);
        mSelectParamBuilder.or(fakeKey2, fakeValue2);

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(2, params.length);
        assertEquals(fakeValue, params[0]);
        assertEquals(fakeValue2, params[1]);
    }

    public void testBuildParams_fiveAndsAndSevensOrs_correctNumberOfParams() {
        for (int i = 0; i < 5; i++) {
            mSelectParamBuilder.and(fakeKey, fakeValue);
        }
        for (int i = 0; i < 7; i++) {
            mSelectParamBuilder.and(fakeKey2, fakeValue2);
        }

        String[] params = mSelectParamBuilder.buildParams();

        assertEquals(12, params.length);
    }
}
