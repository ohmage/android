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
import android.os.RemoteException;

import org.ohmage.helper.SelectParamBuilder;
import org.ohmage.streams.StreamContract;

import java.util.ArrayList;

/**
 * Holds a list of {@link Stream} objects.
 */
public class Streams extends ArrayList<Stream> {

    public Streams() {
    }

    /**
     * Creates a list of all the stream version pairs in the system for all users
     *
     * @param provider
     */
    public Streams(ContentProviderClient provider) {
        this(provider, null, null, null);
    }

    /**
     * Creates a list of all the stream version pairs in the system for the given user
     *
     * @param provider
     * @param account
     */
    public Streams(ContentProviderClient provider, Account account) {
        this(provider, account, null, null);
    }

    /**
     * Creates a list of all the versions of a specific stream for a given user
     *
     * @param provider
     * @param account
     * @param streamId
     */
    public Streams(ContentProviderClient provider, Account account, String streamId) {
        this(provider, account, streamId, null);
    }

    /**
     * Creates a list of all the distinct streams for this account
     *
     * @param provider      queries for streams using the given provider
     * @param account       queries for streams for this account
     * @param streamId      if given, will query for versions of only this stream
     * @param streamVersion if given, will query for a specific stream version pair
     */
    public Streams(ContentProviderClient provider, Account account, String streamId,
            String streamVersion) {
        SelectParamBuilder select = new SelectParamBuilder();

        if (account != null) {
            select.start(StreamContract.Streams.USERNAME, account.name);
        }

        if (streamId != null) {
            select.and(StreamContract.Streams.STREAM_ID, streamId);

            if (streamVersion != null) {
                select.and(StreamContract.Streams.STREAM_VERSION, streamVersion);
            }
        }

        Cursor cursor = null;
        try {
            cursor = provider.query(StreamContract.Streams.CONTENT_URI,
                    new String[]{"distinct " + StreamContract.Streams.STREAM_ID,
                            StreamContract.Streams.STREAM_VERSION},
                    select.buildSelection(), select.buildParams(), null);

            while (cursor.moveToNext()) {
                Stream s = new Stream(cursor.getString(0), cursor.getString(1));
                this.add(s);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}
