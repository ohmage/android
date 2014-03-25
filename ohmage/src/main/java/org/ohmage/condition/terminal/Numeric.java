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
import org.ohmage.condition.OhmageNumber;
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Terminal} that represents some number.
 * </p>
 *
 * @author John
 */
public class Numeric extends Terminal {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.terminal.Numeric} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Terminal.Builder<Numeric> {
        /**
         * The number value.
         */
        private final OhmageNumber value;

        /**
         * Creates a new builder with some number.
         *
         * @param value
         *        The number value.
         */
        public Builder(final Number value) {
            this.value = new OhmageNumber(value);
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
        public Numeric build() throws InvalidArgumentException {
            return new Numeric(value);
        }
    }

    /**
     * The numeric value.
     */
    private final OhmageNumber value;

    /**
     * Creates a new number node.
     *
     * @param value
     *        The value of the number node.
     *
     * @throws InvalidArgumentException
     *         The value was null or did not begin and end with a quotation
     *         mark.
     */
    public Numeric(final OhmageNumber value)
        throws InvalidArgumentException {

        // Be sure it is not null.
        if(value == null) {
            throw new IllegalStateException("The text is null.");
        }

        // Store the value.
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Terminal#getValue(java.util.Map)
     */
    @Override
    public OhmageNumber getValue(final Map<String, Object> responses) {
        return value;
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
     * @return Returns false if the value of this Numeric represents zero.
     *         Otherwise, true is returned.
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return getValue(responses).compareTo(0) == 0;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.terminal.Terminal#lessThanValue(java.util.Map, java.lang.Object)
     */
    @Override
    public boolean lessThanValue(
        final Map<String, Object> responses,
        final Object value) {

        if(value instanceof Number) {
            return this.value.compareTo((Number) value) < 0;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.terminal.Terminal#equalsValue(java.util.Map, java.lang.Object)
     */
    @Override
    public boolean equalsValue(
        final Map<String, Object> responses,
        final Object value) {

        if(value instanceof Number) {
            return this.value.compareTo((Number) value) == 0;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.terminal.Terminal#greaterThanValue(java.util.Map, java.lang.Object)
     */
    @Override
    public boolean greaterThanValue(
        final Map<String, Object> responses,
        final Object value) {

        if(value instanceof Number) {
            return this.value.compareTo((Number) value) > 0;
        }

        return false;
    }
}