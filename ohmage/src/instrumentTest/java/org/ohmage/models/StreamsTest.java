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

package org.ohmage.models;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.test.AndroidTestCase;

import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.streams.StreamContract;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Streams} for building lists of distinct streams in the system
 */
public class StreamsTest extends AndroidTestCase {

    private ContentProviderClient fakeContentProviderClient;

    Account fakeAccount = new Account("name", "type");

    String fakeStreamId = "fakeStreamId";

    String fakeStreamVersion = "fakeStreamVersion";

    SelectParamBuilder select = new SelectParamBuilder();

    String[] COLUMNS = new String[]{
            StreamContract.Streams.STREAM_ID,
            StreamContract.Streams.STREAM_VERSION
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeContentProviderClient = mock(ContentProviderClient.class);
    }

    public void testStreams_withProvider_queriesDbForAllDistinctStreamVersionPairs()
            throws Exception {
        Cursor fakeCursor = new MatrixCursor(COLUMNS);
        when(fakeContentProviderClient.query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION}, "", new String[]{}, null))
                .thenReturn(fakeCursor);

        new Streams(fakeContentProviderClient);

        verify(fakeContentProviderClient).query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION}, "", new String[]{}, null);
    }

    public void testStreams_withProviderAndUser_queriesDbForAllDistinctStreamVersionPairsForUser()
            throws Exception {
        select.and(StreamContract.Streams.USERNAME, fakeAccount.name);
        Cursor fakeCursor = new MatrixCursor(COLUMNS);
        when(fakeContentProviderClient.query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null))
                .thenReturn(fakeCursor);

        new Streams(fakeContentProviderClient, fakeAccount);

        verify(fakeContentProviderClient).query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null);
    }

    public void testStreams_withProviderAndUserAndStream_queriesDbForAllVersionsOfStreamForUser()
            throws Exception {
        select.and(StreamContract.Streams.USERNAME, fakeAccount.name);
        select.and(StreamContract.Streams.STREAM_ID, fakeStreamId);
        Cursor fakeCursor = new MatrixCursor(COLUMNS);
        when(fakeContentProviderClient.query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null))
                .thenReturn(fakeCursor);

        new Streams(fakeContentProviderClient, fakeAccount, fakeStreamId);

        verify(fakeContentProviderClient).query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null);
    }

    public void testStreams_withProviderAndUserAndStreamAndVersion_queriesDbForSingleStreamForUser()
            throws Exception {
        select.and(StreamContract.Streams.USERNAME, fakeAccount.name);
        select.and(StreamContract.Streams.STREAM_ID, fakeStreamId);
        select.and(StreamContract.Streams.STREAM_VERSION, fakeStreamVersion);
        Cursor fakeCursor = new MatrixCursor(COLUMNS);
        when(fakeContentProviderClient.query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null))
                .thenReturn(fakeCursor);

        new Streams(fakeContentProviderClient, fakeAccount, fakeStreamId, fakeStreamVersion);

        verify(fakeContentProviderClient).query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION},
                select.buildSelection(), select.buildParams(), null);
    }

    public void testStreams_withTenPoints_createsStreamWithTenPoints() throws Exception {
        MatrixCursor fakeCursor = new MatrixCursor(COLUMNS);
        for (int i = 0; i < 10; i++) {
            fakeCursor.addRow(new Object[]{"", ""});
        }
        when(fakeContentProviderClient.query(StreamContract.Streams.CONTENT_URI,
                new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                        StreamContract.Streams.STREAM_VERSION}, "", new String[]{}, null))
                .thenReturn(fakeCursor);

        Streams s = new Streams(fakeContentProviderClient);

        assertEquals(10, s.size());
    }
}
