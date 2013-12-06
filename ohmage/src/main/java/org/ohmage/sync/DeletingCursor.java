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
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;

import java.util.LinkedList;

/**
 * A cursor that handles deletions while the data is still being read. The normal cursor will
 * not update its position correctly
 */
public class DeletingCursor extends CursorWrapper {

    private final LinkedList<Long> ids = new LinkedList<Long>();

    private final int mCount;

    private int mOffset = 0;

    /**
     * The number of deleted points
     */
    private int mDeleted;

    public DeletingCursor(Cursor c) {
        super(c);
        mCount = super.getCount();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public int getPosition() {
        return super.getPosition() + mOffset;
    }

    @Override
    public boolean move(int offset) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean moveToPosition(int position) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean moveToFirst() {
        if (!isBeforeFirst())
            throw new RuntimeException("DeletingCursor can only move forwards");
        return super.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean moveToNext() {
        if (!isAfterLast() && !isLast() && super.getPosition() + 1 > getActualCount()) {
            requery();
        }
        if (super.moveToNext()) {
            ids.add(getLong(0));
            return true;
        }
        return false;
    }

    @Override
    public boolean moveToPrevious() {
        throw new RuntimeException("DeletingCursor can only move forwards");
    }

    @Override
    public boolean isFirst() {
        return super.isFirst() && !deletedPoints();
    }

    @Override
    public boolean isBeforeFirst() {
        return super.isBeforeFirst() && !deletedPoints();
    }

    /**
     * Calculates the actual number of points that have not been deleted yet
     *
     * @return the number of points
     */
    public int getActualCount() {
        return super.getCount() - mDeleted;
    }

    /**
     * Check if this cursor has deleted points
     *
     * @return true if it has deleted points
     */
    public boolean deletedPoints() {
        return !ids.isEmpty() || mDeleted != 0;
    }

    /**
     * Delete all points that have been marked so far
     *
     * @param provider
     * @param contentUri
     * @return
     * @throws android.os.RemoteException
     */
    public int deleteMarked(ContentProviderClient provider, Uri contentUri)
            throws RemoteException {
        StringBuilder deleteString = new StringBuilder();

        // Deleting this batch of points. We can only delete
        // with a maximum expression tree depth of 1000
        int batch = 0;
        for (Long id : ids) {
            if (deleteString.length() != 0)
                deleteString.append(" OR ");
            deleteString.append(BaseColumns._ID + "=" + id);
            batch++;

            // If we have 1000 Expressions or we are at the last
            // point, delete them
            if ((batch % (1000 - 2) == 0) || batch == ids.size()) {
                provider.delete(contentUri, deleteString.toString(), null);
                deleteString = new StringBuilder();
            }
        }
        int count = ids.size();
        mDeleted += count;
        ids.clear();
        return count;
    }

    @Override
    @Deprecated
    public boolean requery() {
        int position = super.getPosition();
        if (super.requery()) {
            super.move(position - mDeleted + 1);
            mOffset += mDeleted;
            mDeleted = 0;
            return true;
        }
        return false;
    }
}