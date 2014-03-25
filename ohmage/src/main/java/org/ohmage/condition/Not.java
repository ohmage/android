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

package org.ohmage.condition;

import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Fragment} that reverses the result of a sub-fragment.
 * </p>
 *
 * @author John Jenkins
 */
public class Not extends Fragment {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.Not} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Fragment.Builder<Not> {
        /**
         * The sub-fragment, whose result will be inverted.
         */
        private Fragment.Builder<?> fragment = null;

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(fragment == null) {
                fragment = other;
                return this;
            }
            else {
                return other.merge(this);
            }
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#build()
         */
        @Override
        public Not build() throws InvalidArgumentException {
            return new Not(fragment.build());
        }
    }

    /**
     * The string value of an {@link org.ohmage.condition.Not} within a condition sentence.
     */
    public static final String VALUE = "!";

    /**
     * The JSON key for the fragment.
     */
    public static final String JSON_KEY_NOT = "not";

    /**
     * The fragment that this not inverts.
     */
    private final Fragment fragment;

    /**
     * Creates a new Not backed by some fragment.
     *
     * @param fragment
     *        The fragment that backs this not.
     *
     * @throws InvalidArgumentException
     *         The underlying fragment was null.
     */
    public Not(
        final Fragment fragment)
        throws InvalidArgumentException {

        if(fragment == null) {
            throw
                new InvalidArgumentException(
                    "The fragment is null.");
        }

        this.fragment = fragment;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#validate(java.util.Map)
     */
    @Override
    public void validate(final Map<String, Prompt> surveyItems)
        throws InvalidArgumentException {

        fragment.validate(surveyItems);
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#evaluate(java.util.Map)
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return ! fragment.evaluate(responses);
    }
}