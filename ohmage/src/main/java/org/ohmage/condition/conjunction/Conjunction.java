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

package org.ohmage.condition.conjunction;

import org.ohmage.condition.Fragment;

/**
 * <p>
 * A {@link Fragment} that represents combining two boolean expressions.
 * <p>
 *
 * @author John Jenkins
 */
public abstract class Conjunction extends Fragment {
    /**
     * <p>
     * A marker interface for builders for {@link org.ohmage.condition.conjunction.Conjunction}s that extends
     * the {@link Fragment.Builder}.
     * </p>
     *
     * @author John Jenkins
     */
    public static interface Builder<T extends Conjunction>
        extends Fragment.Builder<T> {}

    /**
     * Parses a word into its appropriate Conjunction value or returns null
     * if no corresponding Conjunction exists.
     *
     * @param word
     *        The word to parse into the Conjunction.
     *
     * @return The Conjunction that represents this word or null if no such
     *         Conjunction exists.
     */
    public static Fragment.Builder<? extends Conjunction> parseWord(
        final String word) {

        if (word.equals(And.VALUE)) {
            return new And.Builder();
        } else if (word.equals(Or.VALUE)) {
            return new Or.Builder();
        }

        return null;
    }
}