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

import java.util.LinkedList;

/**
 * Helper for making select statements to query a content provider.
 */
public class SelectParamBuilder {

    private final StringBuilder selection;
    private final LinkedList<String> params;

    public final static String AND = " AND ";
    public final static String OR = " OR ";
    private boolean mNegate = false;

    public SelectParamBuilder() {
        selection = new StringBuilder();
        params = new LinkedList<String>();
    }

    public SelectParamBuilder(String key, String value) {
        selection = new StringBuilder(key);
        params = new LinkedList<String>();
        params.add(value);
    }

    /**
     * Convenience method to add a the first value since it wont be an AND or an OR. Calling
     * either {@link #and(String, Object)} or {@link #or(String, Object)} has the same effect.
     *
     * @param key
     * @param value
     * @return
     */
    public SelectParamBuilder start(String key, String value) {
        if (size() > 0) {
            throw new RuntimeException("start must be called before and() or or()");
        }
        append("", key, value);
        return this;
    }

    /**
     * Append a key and value to the selection with an appended AND if there are parameters before.
     *
     * @param key
     * @param value
     * @return
     */
    public SelectParamBuilder and(String key, Object value) {
        return append(AND, key, value);
    }

    /**
     * Append a key and value to the selection with an appended OR if there are parameters before.
     *
     * @param key
     * @param value
     * @return
     */
    public SelectParamBuilder or(String key, Object value) {
        return append(OR, key, value);
    }

    private SelectParamBuilder append(String operator, String key, Object value) {
        if (selection.length() != 0) {
            selection.append(operator);
        }
        selection.append(key).append("=?");
        params.add(value.toString());
        return this;
    }

    /**
     * Append an AND subselection surrounded by parenthesis
     *
     * @param subSelect
     * @return
     */
    public SelectParamBuilder andSubSelect(SelectParamBuilder subSelect) {
        return appendSubSelect(AND, subSelect);
    }

    /**
     * Append an OR subselection surrounded by parenthesis
     *
     * @param subSelect
     * @return
     */
    public SelectParamBuilder orSubSelect(SelectParamBuilder subSelect) {
        return appendSubSelect(OR, subSelect);
    }

    private SelectParamBuilder appendSubSelect(String operator, SelectParamBuilder subSelect) {
        if (selection.length() != 0) {
            selection.append(operator);
        }
        selection.append("(").append(subSelect.buildSelection()).append(")");
        params.addAll(subSelect.params);
        return this;
    }

    public SelectParamBuilder negate() {
        return negate(true);
    }

    public SelectParamBuilder negate(boolean negate) {
        mNegate = negate;
        return this;
    }

    /**
     * Builds the selection string
     *
     * @return
     */
    public String buildSelection() {
        if(mNegate && selection.length() > 0) {
            selection.insert(0, "NOT (").append(")");
        }
        return selection.toString();
    }

    /**
     * Builds the params array
     *
     * @return
     */
    public String[] buildParams() {
        return params.toArray(new String[]{});
    }

    /**
     * Returns the number of key value pairs added to the builder.
     *
     * @return
     */
    public int size() {
        return params.size();
    }
}
