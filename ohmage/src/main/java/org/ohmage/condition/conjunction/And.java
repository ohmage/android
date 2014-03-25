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
import org.ohmage.condition.InvalidArgumentException;
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Conjunction} that represents true when all of its children
 * represent true.
 * </p>
 *
 * @author John Jenkins
 */
public class And extends Conjunction {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.conjunction.And} objects.
     * <p>
     *
     * @author John Jenkins
     */
    public static class Builder implements Conjunction.Builder<And> {
        /**
         * The builder for the left operand.
         */
        private Fragment.Builder<?> left;
        /**
         * The builder for the right operand.
         */
        private Fragment.Builder<?> right;

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(left == null) {
                left = other;
            }
            else if(right == null) {
                right = other;
            }
            else {
                right = right.merge(other);
            }

            return this;
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#build()
         */
        @Override
        public And build() throws InvalidArgumentException {
            if(left == null) {
                throw
                    new InvalidArgumentException(
                        "The 'and' operator does not have a left operand.");
            }
            if(right == null) {
                throw
                    new InvalidArgumentException(
                        "The 'and' operator does not have a right operand.");
            }
            return new And(left.build(), right.build());
        }
    }

    /**
     * The string value of an {@link org.ohmage.condition.conjunction.And} within a condition sentence.
     */
    public static final String VALUE = "AND";

    /**
     * The left operand.
     */
    private final Fragment left;
    /**
     * The right operand.
     */
    private final Fragment right;

    /**
     * Creates a new And object with left and right operands.
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
    public And(
        final Fragment left,
        final Fragment right)
        throws InvalidArgumentException {

        if(left == null) {
            throw
                new InvalidArgumentException(
                    "The left operand is missing.");
        }
        if(right == null) {
            throw
                new InvalidArgumentException(
                    "The right operand is missing.");
        }

        this.left = left;
        this.right = right;
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
        return left.evaluate(responses) && right.evaluate(responses);
    }
}