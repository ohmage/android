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

package org.ohmage.sync;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.test.AndroidTestCase;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ohmage.models.Stream;
import org.ohmage.models.Streams;
import org.ohmage.streams.StreamContract;
import org.ohmage.streams.StreamPointBuilder;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link org.ohmage.sync.StreamWriterOutput} which will stream points from the db to the
 * network in batches.
 */
public class StreamWriterOutputTest extends AndroidTestCase {

    private ContentProviderClient fakeContentProviderClient;

    private StreamWriterOutput mStreamWriterOutput;

    String[] PROJECTION = new String[]{
            StreamContract.Streams._ID,
            StreamContract.Streams.STREAM_METADATA,
            StreamContract.Streams.STREAM_DATA
    };

    protected static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeContentProviderClient = mock(ContentProviderClient.class);

        mStreamWriterOutput = new StreamWriterOutput(fakeContentProviderClient);
    }

    public void testQuery_withNameAndStream_QueriesForStreamsWithUserForStream() throws Exception {
        String fakeName = "fakeName";
        Stream fakeStream = new Stream("fakeId", "fakeVersion");
        when(fakeContentProviderClient.query(eq(StreamContract.Streams.CONTENT_URI),
                any(String[].class), eq(StreamContract.Streams.USERNAME + "=? AND "
                                        + StreamContract.Streams.STREAM_ID + "=? AND "
                                        + StreamContract.Streams.STREAM_VERSION + "=?"),
                eq(new String[]{fakeName, fakeStream.id, fakeStream.version}), any(String.class)))
                .thenReturn(new MatrixCursor(PROJECTION));

        mStreamWriterOutput.query(fakeName, fakeStream);

        verify(fakeContentProviderClient).query(eq(StreamContract.Streams.CONTENT_URI),
                any(String[].class), eq(StreamContract.Streams.USERNAME + "=? AND "
                                        + StreamContract.Streams.STREAM_ID + "=? AND "
                                        + StreamContract.Streams.STREAM_VERSION + "=?"),
                eq(new String[]{fakeName, fakeStream.id, fakeStream.version}), any(String.class));
    }

    public void testSetCursor_hasCursor_closesOldCursor() {
        Cursor fakeCursor = mock(Cursor.class);
        mStreamWriterOutput.setCursor(fakeCursor);

        mStreamWriterOutput.setCursor(mock(Cursor.class));

        verify(fakeCursor).close();
    }

    public void testMoveToNextBatch_noCursor_returnsFalse() throws Exception {
        mStreamWriterOutput.setCursor(null);

        boolean result = mStreamWriterOutput.moveToNextBatch();

        assertFalse(result);
    }

    public void testMoveToNextBatch_hasPointsToDelete_throwsException() throws Exception {
        DeletingCursor fakeCursor = mock(DeletingCursor.class);
        mStreamWriterOutput.setCursor(fakeCursor);
        when(fakeCursor.hasDeletedPoints()).thenReturn(true);

        try {
            mStreamWriterOutput.moveToNextBatch();
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
        }
    }

    public void testMoveToNextBatch_hasCursor_checksIfCursorIsAtEnd() throws Exception {
        DeletingCursor fakeCursor = mock(DeletingCursor.class);
        mStreamWriterOutput.setCursor(fakeCursor);

        mStreamWriterOutput.moveToNextBatch();

        verify(fakeCursor).isLast();
    }

    public void testMoveToNextBatch_hasCursorNotAtLast_returnsTrue() throws Exception {
        DeletingCursor fakeCursor = mock(DeletingCursor.class);
        mStreamWriterOutput.setCursor(fakeCursor);
        when(fakeCursor.isLast()).thenReturn(false);

        boolean ret = mStreamWriterOutput.moveToNextBatch();

        assertTrue(ret);
    }

    public void testDeleteBatch_noCursor_returnsZero() throws Exception {
        mStreamWriterOutput.setCursor(null);

        int res = mStreamWriterOutput.deleteBatch();

        assertEquals(0, res);
    }

    public void testDeleteBatch_hasCursor_deletesMarkedPoints() throws Exception {
        DeletingCursor fakeCursor = mock(DeletingCursor.class);
        mStreamWriterOutput.setCursor(fakeCursor);

        mStreamWriterOutput.deleteBatch();

        verify(fakeCursor).deleteMarked(fakeContentProviderClient,
                StreamContract.Streams.CONTENT_URI);
    }

    public void testWriteTo_noCursorSet_throwsException()
            throws Exception {
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
            }
        };

        try {
            mStreamWriterOutput.writeTo(fakeOutputStream);
            fail("No Exception Thrown");
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
        }
    }

    public void testWriteTo_sumOfPointsLargerThanMaxBytes_writesABatchOfPoints() throws Exception {
        Cursor fakeCursor = mock(Cursor.class);
        when(fakeCursor.moveToNext()).thenReturn(true);
        mStreamWriterOutput.setCursor(fakeCursor);
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
            }
        };
        mStreamWriterOutput.moveToNextBatch();

        mStreamWriterOutput.writeTo(fakeOutputStream);

        verify(fakeCursor, times(StreamWriterOutput.BATCH_MAX_COUNT)).moveToNext();
    }

    public void testWriteTo_sumOfBytesLargerThanMaxPoints_writesABatchOfPoints() throws Exception {
        Cursor fakeCursor = mock(Cursor.class);
        when(fakeCursor.moveToNext()).thenReturn(true);
        final int numPoints = 10;
        when(fakeCursor.getString(anyInt())).then(new Answer<String>() {
            @Override public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                int size = (int) StreamWriterOutput.BATCH_MAX_SIZE_BYTES / 2 / numPoints;
                StringBuilder sb = new StringBuilder(size);
                for (int i = 0; i < size; i++) {
                    sb.append('a');
                }
                return sb.toString();
            }
        });
        mStreamWriterOutput.setCursor(fakeCursor);
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
            }
        };
        mStreamWriterOutput.moveToNextBatch();

        mStreamWriterOutput.writeTo(fakeOutputStream);

        verify(fakeCursor, times(numPoints)).moveToNext();
    }

    public void testWriteTo_hasSpecialCharacters_writesCorrectNumberOfPointsPerBatch()
            throws Exception {
        Cursor fakeCursor = mock(Cursor.class);
        when(fakeCursor.moveToNext()).thenReturn(true);
        final int numPoints = 10;
        when(fakeCursor.getString(anyInt())).then(new Answer<String>() {
            @Override public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                int size = (int) StreamWriterOutput.BATCH_MAX_SIZE_BYTES / 2 / numPoints / 2;
                // This uses a character which takes up two bytes instead of one
                StringBuilder sb = new StringBuilder(size);
                for (int i = 0; i < size; i++) {
                    sb.append('©');
                }
                return sb.toString();
            }
        });
        mStreamWriterOutput.setCursor(fakeCursor);
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
            }
        };
        mStreamWriterOutput.moveToNextBatch();

        mStreamWriterOutput.writeTo(fakeOutputStream);

        verify(fakeCursor, times(numPoints)).moveToNext();
    }

    public void testWriteTo_hasSinglePoint_writesPointJson() throws Exception {
        MatrixCursor fakeCursor = new MatrixCursor(PROJECTION);
        StreamPointBuilder builder = new StreamPointBuilder();
        Stream point = new Stream();
        point.data = "{}";
        point.metaData = builder.withTime("timestamp").withId().getMetadata();
        fakeCursor.addRow(new Object[]{0, point.metaData, point.data});
        mStreamWriterOutput.setCursor(fakeCursor);
        final StringBuilder output = new StringBuilder();
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
                output.append((char) oneByte);
            }
        };
        mStreamWriterOutput.moveToNextBatch();

        mStreamWriterOutput.writeTo(fakeOutputStream);

        Object[] streams = gson.fromJson(output.toString(), Object[].class);
        assertEquals(1, streams.length);
        LinkedTreeMap pointStream = (LinkedTreeMap) streams[0];
        assertEquals(point.data, gson.toJson(pointStream.get("data")));
        assertEquals(point.metaData, gson.toJson(pointStream.get("meta_data")));
    }

    public void testWriteTo_hasMultiplePoints_writesPointsJson() throws Exception {
        MatrixCursor fakeCursor = new MatrixCursor(PROJECTION);
        StreamPointBuilder builder = new StreamPointBuilder();
        Streams points = new Streams();
        for (int i = 0; i < 100; i++) {
            Stream point = new Stream();
            point.data = "{}";
            point.metaData = builder.withTime("timestamp").withId().getMetadata();
            points.add(point);
            fakeCursor.addRow(new Object[]{i, point.metaData, point.data});
        }
        mStreamWriterOutput.setCursor(fakeCursor);
        final StringBuilder output = new StringBuilder();
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
                output.append((char) oneByte);
            }
        };
        mStreamWriterOutput.moveToNextBatch();

        mStreamWriterOutput.writeTo(fakeOutputStream);

        Object[] streams = gson.fromJson(output.toString(), Object[].class);
        assertEquals(100, streams.length);
        for (int i = 0; i < 100; i++) {
            LinkedTreeMap pointStream = (LinkedTreeMap) streams[i];
            assertEquals(points.get(i).data, gson.toJson(pointStream.get("data")));
            assertEquals(points.get(i).metaData, gson.toJson(pointStream.get("meta_data")));
        }
    }

    public void testWriteTo_beforeMovingToFirstBatch_DoesNotWritePoints() throws Exception {
        mStreamWriterOutput.setCursor(mock(Cursor.class));
        OutputStream fakeOutputStream = mock(OutputStream.class);

        mStreamWriterOutput.writeTo(fakeOutputStream);

        verifyZeroInteractions(fakeOutputStream);
    }

    public void testWriteTo_afterWriteToAlreadyCalled_DoesNotWritePoints() throws Exception {
        Cursor fakeCursor = mock(Cursor.class);
        when(fakeCursor.moveToNext()).thenReturn(true);
        mStreamWriterOutput.setCursor(fakeCursor);
        OutputStream fakeOutputStream = new OutputStream() {
            @Override public void write(int oneByte) throws IOException {
            }
        };
        mStreamWriterOutput.writeTo(fakeOutputStream);
        fakeOutputStream = mock(OutputStream.class);

        mStreamWriterOutput.writeTo(fakeOutputStream);

        verifyZeroInteractions(fakeOutputStream);
    }
}
