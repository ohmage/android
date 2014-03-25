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

import org.ohmage.condition.comparator.Comparator;
import org.ohmage.condition.terminal.Terminal;
import org.ohmage.prompts.Prompt;

import java.util.Map;


/**
 * <p>
 * A {@link Fragment} that represents an enclosed condition that should not
 * be modified.
 * </p>
 *
 * @author John Jenkins
 */
public class Parenthetical extends Fragment {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.Parenthetical} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder
        implements Fragment.Builder<Parenthetical> {

        /**
         * The condition that defines the contents within the parenthesis.
         */
        private final Condition condition;

        /**
         * Creates a new builder with some condition.
         *
         * @param condition
         *        The condition.
         */
        public Builder(final Condition condition) {
            this.condition = condition;
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(other instanceof Terminal.Builder<?>) {
                throw
                    new InvalidArgumentException(
                        "A parenthetical cannot be merged with a " +
                            "terminal.");
            }
            else if(other instanceof Comparator.Builder<?>) {
                throw
                    new InvalidArgumentException(
                        "A parenthetical cannot be merged with a " +
                            "compartor.");
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
        public Parenthetical build() throws InvalidArgumentException {
            return new Parenthetical(condition);
        }
    }

    /**
     * An opening parenthesis.
     */
    public static final char START = '(';
    /**
     * A closing parenthesis.
     */
    public static final char END = ')';

    /**
     * The condition that makes up this parenthetical.
     */
    private final Condition condition;

    /**
     * Creates a new parenthetical backed by some condition.
     *
     * @param condition
     *        The condition that backs this parenthetical.
     *
     * @throws InvalidArgumentException
     *         The underlying condition was null.
     */
    public Parenthetical(
        final Condition condition)
        throws InvalidArgumentException {

        if(condition == null) {
            throw
                new InvalidArgumentException(
                    "The parenthetical condition is null.");
        }

        this.condition = condition;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#validate(java.util.Map)
     */
    @Override
    public void validate(final Map<String, Prompt> surveyItems)
        throws InvalidArgumentException {

        condition.validate(surveyItems);
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#evaluate(java.util.Map)
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return condition.evaluate(responses);
    }
}