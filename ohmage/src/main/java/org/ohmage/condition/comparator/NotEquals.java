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
import org.ohmage.condition.InvalidArgumentException;
import org.ohmage.condition.terminal.Terminal;
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Comparator} that returns true when two {@link Terminals}
 * evaluate to different values.
 * </p>
 *
 * @author John Jenkins
 */
public class NotEquals extends Comparator {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.comparator.NotEquals} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Comparator.Builder<NotEquals> {
        /**
         * The builder for the left operand.
         */
        private Terminal.Builder<?> left;
        /**
         * The builder for the right operand.
         */
        private Terminal.Builder<?> right;

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(other instanceof Terminal.Builder<?>) {
                if(left == null) {
                    left = (Terminal.Builder<?>) other;
                }
                else if(right == null) {
                    right = (Terminal.Builder<?>) other;
                }
                else {
                    throw
                        new InvalidArgumentException(
                            "Multiple terminals are in sequence.");
                }

                return this;
            }
            else if(other instanceof Comparator.Builder<?>) {
                throw
                    new InvalidArgumentException(
                        "A comparator cannot be compared to another " +
                            "comparator.");
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
        public NotEquals build() throws InvalidArgumentException {
            if(left == null) {
                throw
                    new InvalidArgumentException(
                        "The 'not equals' does not have a left operand.");
            }
            if(right == null) {
                throw
                    new InvalidArgumentException(
                        "The 'not equals' does not have a right operand.");
            }

            return new NotEquals(left.build(), right.build());
        }
    }

    /**
     * The string value of a {@link org.ohmage.condition.comparator.NotEquals} within a condition sentence.
     */
    public static final String VALUE = "!=";

    /**
     * The left operand.
     */
    private final Terminal left;
    /**
     * The right operand.
     */
    private final Terminal right;

    /**
     * Creates a new NotEquals object with left and right operands.
     *
     * @param left
     *        The left operand.
     *
     * @param right
     *        The right operand.
     *
     * @throws InvalidArgumentException
     *         One or both of the operands is null.
     */
    public NotEquals(final Fragment left, final Fragment right)
        throws InvalidArgumentException {

        if(left == null) {
            throw
                new InvalidArgumentException(
                    "The left operand is missing.");
        }
        if(! (left instanceof Terminal)) {
            throw
            new InvalidArgumentException(
                "The left operand is not a terminal value.");
        }
        if(right == null) {
            throw
                new InvalidArgumentException(
                    "The right operand is missing.");
        }
        if(! (right instanceof Terminal)) {
            throw
            new InvalidArgumentException(
                "The right operand is not a terminal value.");
        }

        this.left = (Terminal) left;
        this.right = (Terminal) right;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#validate(java.util.Map)
     */
    @Override
    public void validate(final Map<String, Prompt> surveyItems)
        throws InvalidArgumentException {

        left.validate(surveyItems);
        right.validate(surveyItems);
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#evaluate(java.util.Map)
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        return ! left.equalsValue(responses, right.getValue(responses));
    }
}