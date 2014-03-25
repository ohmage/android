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

/**
 * <p>
 * The possible values when a prompt does not have a response.
 * </p>
 *
 * @author John Jenkins
 */
public enum NoResponse {
    /**
     * Based on the condition associated with the prompt and the previous
     * prompt responses, this prompt should not have been displayed.
     */
    NOT_DISPLAYED,
    /**
     * The prompt was skipped by the user.
     */
    SKIPPED;
}
