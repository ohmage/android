/*
 * Copyright (C) 2013 ohmage
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

package org.ohmage.models;

import java.util.ArrayList;
import java.util.Date;

/**
 * Basic User class
 */
public class User {
    public String email;

    public String fullName;

    public ArrayList<OhmageAuthProvider> providers;

    public OhmageRegistration registration;

    public Date activationTimestamp;

    public static class OhmageRegistration {
        public String userId;

        public String email;

        public String activationId;

        public String activationTimestamp;
    }

    public static class OhmageAuthProvider {
        public String userId;

        public String email;

        public String providerId;
    }

    // ohmlets
    // streams
    // surveys
}
