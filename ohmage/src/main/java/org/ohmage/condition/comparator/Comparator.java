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

package org.ohmage.condition.comparator;

import org.ohmage.condition.Fragment;

/**
 * <p>
 * A {@link Fragment} that represents comparing two {@link org.ohmage.condition.terminal.Terminal} values.
 * </p>
 *
 * @author John Jenkins
 */
public abstract class Comparator extends Fragment {
    /**
     * <p>
     * A marker interface for builders for {@link org.ohmage.condition.comparator.Comparator}s that extends the
     * {@link Fragment.Builder}.
     * </p>
     *
     * @author John Jenkins
     */
    public static interface Builder<T extends Comparator>
        extends Fragment.Builder<T> {}

    /**
     * Parses a word into its appropriate Comparator value or returns null
     * if no corresponding Comparator exists.
     *
     * @param word
     *        The word to parse into the Comparator.
     *
     * @return The Comparator that represents this word or null if no such
     *         Comparator exists.
     */
    public static Fragment.Builder<?> parseWord(final String word) {
        if (word.equals(Equals.VALUE)) {
            return new Equals.Builder();
        } else if (word.equals(NotEquals.VALUE)) {
            return new NotEquals.Builder();
        } else if (word.equals(LessThan.VALUE)) {
            return new LessThan.Builder();
        } else if (word.equals(LessThanEquals.VALUE)) {
            return new LessThanEquals.Builder();
        } else if (word.equals(GreaterThan.VALUE)) {
            return new GreaterThan.Builder();
        } else if (word.equals(GreaterThanEquals.VALUE)) {
            return new GreaterThanEquals.Builder();
        }

        return null;
    }
}