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

package org.ohmage.condition.terminal;

import org.ohmage.condition.Fragment;
import org.ohmage.condition.InvalidArgumentException;
import org.ohmage.condition.NoResponse;
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Terminal} that represents skipping a prompt.
 * </p>
 *
 * @author John Jenkins
 */
public class Skipped extends Terminal {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.terminal.Skipped} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Terminal.Builder<Skipped> {
        /**
         * Creates a new builder.
         */
        public Builder() {
            // Do nothing.
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(other instanceof Terminal.Builder<?>) {
                throw
                    new InvalidArgumentException(
                        "More than one terminals in a row are not " +
                            "allowed.");
            }

            return other.merge(this);
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Fragment.Builder#build()
         */
        @Override
        public Skipped build() throws InvalidArgumentException {
            return new Skipped();
        }
    }

    /**
     * Creates a new "skipped" node.
     */
    public Skipped() {
        // Do nothing.
    }

    /**
     * @return Always returns {@link NoResponse#SKIPPED}.
     */
    @Override
    public Object getValue(final Map<String, Object> responses) {
        return NoResponse.SKIPPED;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#validate(java.util.Map)
     */
    @Override
    public void validate(final Map<String, Prompt> surveyItems)
        throws InvalidArgumentException {

        // Do nothing.
    }

    /**
     * @return Always returns false.
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return false;
    }

    /**
     * @return Always returns false.
     */
    @Override
    public boolean lessThanValue(
        final Map<String, Object> responses,
        final Object value) {

        return false;
    }

    /**
     * Compares the 'value' to {@link NoResponse#SKIPPED}.
     */
    @Override
    public boolean equalsValue(
        final Map<String, Object> responses,
        final Object value) {

        return NoResponse.SKIPPED.equals(value);
    }

    /**
     * @return Always returns false.
     */
    @Override
    public boolean greaterThanValue(
        final Map<String, Object> responses,
        final Object value) {

        return false;
    }
}