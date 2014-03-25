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
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Terminal} that represents some quoted text.
 * </p>
 *
 * @author John Jenkins
 */
public class Text extends Terminal {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.terminal.Text} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Terminal.Builder<Text> {
        /**
         * The quoted text value.
         */
        private final String value;

        /**
         * Creates a new builder with some quoted text value.
         *
         * @param value
         *        The quoted text value.
         */
        public Builder(final String value) {
            this.value = value;
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
        public Text build() throws InvalidArgumentException {
            return new Text(value);
        }
    }

    /**
     * The text value.
     */
    private final String value;

    /**
     * Creates a new text node.
     *
     * @param value
     *        The value of the text node.
     *
     * @throws InvalidArgumentException
     *         The value was null or did not begin and end with a quotation
     *         mark.
     */
    public Text(final String value)
        throws InvalidArgumentException {

        // Be sure it is not null.
        if(value == null) {
            throw new IllegalStateException("The text is null.");
        }

        // Be sure it begins with a quote.
        if(! value.startsWith("\"")) {
            throw
                new InvalidArgumentException(
                    "Text values must begin with a \".");
        }

        // Be sure it ends with a quote.
        if(! value.endsWith("\"")) {
            throw
                new InvalidArgumentException(
                    "Text values must end with a \".");
        }

        // Remove the quotes.
        this.value = value.substring(1, value.length() - 1);
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Terminal#getValue(java.util.Map)
     */
    @Override
    public String getValue(final Map<String, Object> responses) {
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
     * @return Always returns true.
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return true;
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

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.terminal.Terminal#equalsValue(java.util.Map, java.lang.Object)
     */
    @Override
    public boolean equalsValue(
        final Map<String, Object> responses,
        final Object value) {

        return value.equals(value);
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