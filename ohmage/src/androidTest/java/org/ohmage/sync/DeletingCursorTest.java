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

package org.ohmage.sync;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the deleting cursor
 */
public class DeletingCursorTest extends AndroidTestCase {

    private DeletingCursor mDeletingCursor;

    private DeletingCursor mDeletingCursorWithPoints;

    private Cursor fakeCursor;

    private MatrixCursor dataCursor;

    private ContentProviderClient fakeContentProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeContentProvider = mock(ContentProviderClient.class);

        fakeCursor = mock(Cursor.class);

        mDeletingCursor = new DeletingCursor(fakeCursor);

        dataCursor = new MatrixCursor(new String[]{"id"});
        for (int i = 0; i < 10; i++)
            dataCursor.addRow(new Object[]{0l});

        mDeletingCursorWithPoints = new DeletingCursor(dataCursor);
    }

    public void testRestart_afterMovingButBeforePointsHaveBeenDeleted_callsMoveToFirst() {
        when(fakeCursor.isAfterLast()).thenReturn(false);
        when(fakeCursor.moveToNext()).thenReturn(true);
        mDeletingCursor.moveToNext();

        mDeletingCursor.restart();

        verify(fakeCursor).moveToFirst();
    }

    public void testRestart_afterMovingButBeforePointsHaveBeenDeleted_returnsResultFromMemberCursor() {
        when(fakeCursor.isAfterLast()).thenReturn(false);
        when(fakeCursor.moveToNext()).thenReturn(true);
        mDeletingCursor.moveToNext();

        when(fakeCursor.moveToFirst()).thenReturn(true, false);

        assertTrue(mDeletingCursor.restart());
        assertFalse(mDeletingCursor.restart());
    }

    public void testRestart_afterPointsHaveBeenDeleted_doesNotCallMoveToFirst() throws Exception {
        mDeletingCursorWithPoints.moveToNext();
        mDeletingCursorWithPoints.deleteMarked(fakeContentProvider, Uri.EMPTY);

        mDeletingCursorWithPoints.restart();

        verify(fakeCursor, times(0)).moveToFirst();
    }

    public void testRestart_afterPointsHaveBeenDeleted_returnsFalse() throws Exception {
        mDeletingCursorWithPoints.moveToNext();
        mDeletingCursorWithPoints.deleteMarked(fakeContentProvider, Uri.EMPTY);

        boolean ret = mDeletingCursorWithPoints.restart();

        assertFalse(ret);
    }

    public void testDeletedPoints_afterMovingAndThenRestart_returnsZero() {
        mDeletingCursorWithPoints.moveToNext();
        mDeletingCursorWithPoints.restart();

        assertFalse(mDeletingCursorWithPoints.hasDeletedPoints());
    }
}