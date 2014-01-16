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

package org.ohmage.widget;

import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An array adapter that is backwards compatible to older versions of android
 */
public class CompatArrayAdapter<T> extends ArrayAdapter<T> {

    private boolean mNotifyOnChange = true;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     */
    public CompatArrayAdapter(Context context, int resource) {
        super(context, resource, 0, new ArrayList<T>());
    }

    /**
     * Constructor
     *
     * @param context            The current context.
     * @param resource           The resource ID for a layout file containing a layout to use when
     *                           instantiating views.
     * @param textViewResourceId The ohmletId of the TextView within the layout resource to be populated
     */
    public CompatArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId, new ArrayList<T>());
    }

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public CompatArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, 0, Arrays.asList(objects));
    }

    /**
     * Constructor
     *
     * @param context            The current context.
     * @param resource           The resource ID for a layout file containing a layout to use when
     *                           instantiating views.
     * @param textViewResourceId The ohmletId of the TextView within the layout resource to be populated
     * @param objects            The objects to represent in the ListView.
     */
    public CompatArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public CompatArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, 0, objects);
    }

    /**
     * Constructor
     *
     * @param context            The current context.
     * @param resource           The resource ID for a layout file containing a layout to use when
     *                           instantiating views.
     * @param textViewResourceId The ohmletId of the TextView within the layout resource to be populated
     * @param objects            The objects to represent in the ListView.
     */
    public CompatArrayAdapter(Context context, int resource, int textViewResourceId,
            List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    /**
     * Control whether methods that change the list ({@link #add},
     * {@link #insert}, {@link #remove}, {@link #clear}) automatically call
     * {@link #notifyDataSetChanged}.  If set to false, caller must
     * manually call notifyDataSetChanged() to have the changes
     * reflected in the attached view.
     * <p/>
     * The default is true, and calling notifyDataSetChanged()
     * resets the flag to true.
     *
     * @param notifyOnChange if true, modifications to the list will
     *                       automatically call {@link
     *                       #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        super.setNotifyOnChange(notifyOnChange);
        mNotifyOnChange = notifyOnChange;
    }


    /**
     * Adds the specified Collection at the end of the array.
     *
     * @param collection The Collection to add at the end of the array.
     */
    public void addAll(Collection<? extends T> collection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.addAll(collection);
        } else {
            super.setNotifyOnChange(false);
            for (T object : collection) {
                add(object);
            }
            super.setNotifyOnChange(mNotifyOnChange);
            if (mNotifyOnChange) notifyDataSetChanged();
        }
    }

    /**
     * Adds the specified items at the end of the array.
     *
     * @param items The items to add at the end of the array.
     */
    public void addAll(T... items) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.addAll(items);
        } else {
            super.setNotifyOnChange(false);
            for (T object : items) {
                add(object);
            }
            super.setNotifyOnChange(mNotifyOnChange);
            if (mNotifyOnChange) notifyDataSetChanged();
        }
    }

    /**
     * Replaces the items in the array without notifying the list of the change twice
     *
     * @param collection
     */
    public void replace(Collection<? extends T> collection) {
        super.setNotifyOnChange(false);
        clear();
        super.setNotifyOnChange(mNotifyOnChange);
        if(collection != null)
            addAll(collection);
    }

    /**
     * Replaces the items in the array without notifying the list of the change twice
     *
     * @param items The items to add at the end of the array.
     */
    public void replace(T... items) {
        super.setNotifyOnChange(false);
        clear();
        super.setNotifyOnChange(mNotifyOnChange);
        if(items != null)
            addAll(items);
    }
}
