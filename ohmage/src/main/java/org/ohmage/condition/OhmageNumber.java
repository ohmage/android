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

import java.math.BigDecimal;

/**
 * <p>
 * A generic representation of a number in an attempt to bring order to the
 * chaos.
 * </p>
 *
 * @author John Jenkins
 */
public class OhmageNumber {

    /**
     * The internal representation of the number that backs this object.
     */
    private final BigDecimal internal;

    /**
     * Builds a OhmageNumber from a {@link Numeric} object.
     *
     * @param value
     *        The {@link Numeric} that backs this object.
     */
    public OhmageNumber(final Number value) {
        internal = new BigDecimal(value.toString());
    }

    /**
     * Returns the internal representation of this number.
     *
     * @return The internal representation of this number.
     */
    public BigDecimal getNumber() {
        return internal;
    }

    /**
     * Returns the value of this number before scaling has been applied.
     *
     * @return The value of this number before scaline has been applied.
     */
    public long getUnscaled() {
        return internal.unscaledValue().longValue();
    }

    /**
     * Returns the amount to scale this value.
     *
     * @return The amount to scale this value.
     */
    public int getScale() {
        return internal.scale();
    }

    /*
     * (non-Javadoc)
     * @see java.math.BigDecimal#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if(other instanceof BigDecimal) {
            return internal.compareTo((BigDecimal) other) == 0;
        }
        if(other instanceof OhmageNumber) {
            return internal.compareTo(((OhmageNumber) other).internal) == 0;
        }
        if(other instanceof Number) {
            return compareTo((Number) other) == 0;
        }
        return false;
    }

    /**
     * Compares this OhmageNumber to another Number.
     *
     * @param number
     *        The number to compare to this OhamgeNumber.
     *
     * @return Less than -1 if this OhmageNumber is less than the number, 0 if
     *         this OhmageNumber is equal to the number, or 1 if this Ohmage
     *         number is greater than the number.
     */
    public int compareTo(final Number number) {
        return internal.compareTo((new OhmageNumber(number)).internal);
    }

    /**
     * Compares two {@link Numeric} objects by using their double values.
     *
     * @param first
     *        The first number to compare.
     *
     * @param second
     *        The second number to compare.
     *
     * @return Less than 0 if the first number is less than the second,
     *         greater than 0 if the first number is greater than the
     *         second, or 0 if they represent the same value.
     */
    public static int compareNumbers(
        final Number first,
        final Number second) {

        OhmageNumber firstNumber = new OhmageNumber(first);
        OhmageNumber secondNumber = new OhmageNumber(second);

        return firstNumber.internal.compareTo(secondNumber.internal);
    }
}