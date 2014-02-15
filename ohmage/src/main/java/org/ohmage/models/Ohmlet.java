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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.provider.ContentProviderReader;
import org.ohmage.provider.ContentProviderReader.Readable;
import org.ohmage.provider.OhmageContract;

import java.util.ArrayList;

/**
 * Basic Ohmlet class
 */
public class Ohmlet implements Savable, Readable {
    public String ohmletId;
    public String name;
    public String description;

    public Streams streams;
    public Surveys surveys;
    public Member.List people;
    public PrivacyState privacyState;

    public boolean isMember(String userId) {
        return people.getMember(userId) != null;
    }

    public static class Member {
        public String memberId;
        public Role role;
        public String code;

        public static class List extends ArrayList<Member> {
            public Role getRole(String userId) {
                Member member = getMember(userId);
                if (member != null)
                    return member.role;
                return null;
            }

            public Member getMember(String userId) {
                for (Member member : this) {
                    if (member.memberId.equals(userId))
                        return member;
                }
                return null;
            }

            public void removeMember(String userId) {
                for (Member member : this) {
                    if (member.memberId.equals(userId)) {
                        remove(member);
                        return;
                    }
                }
            }
        }
    }

    public static enum PrivacyState {
        UNKNOWN,
        PRIVATE,
        INVITE_ONLY,
        PUBLIC,
    }

    public static enum Role {
        REQUESTED,
        INVITED,
        MEMBER,
        MODERATOR,
        OWNER;

        /**
         * Returns whether or not this role is the same as or greater than the
         * given role.
         *
         * @param role The role to compare with this role.
         * @return True only if this role is just as privileged or more
         * privileged than the given role.
         */
        public boolean encompases(final Role role) {
            return ordinal() >= role.ordinal();
        }

        /**
         * Returns whether or not this role is greater than the given role.
         *
         * @param role The role to compare with this role.
         * @return True only if this role is more privileged than the given
         * role.
         */
        public boolean supersedes(final Role role) {
            if (role == null) {
                return true;
            }

            return ordinal() > role.ordinal();
        }

    }

    @Override
    public Uri getUrl() {
        return OhmageContract.Ohmlets.CONTENT_URI;
    }

    @Override
    public ContentValues toContentValues(ContentProviderSaver saver) {
        ContentValues values = new ContentValues();
        values.put(OhmageContract.Ohmlets.OHMLET_ID, ohmletId);
        values.put(OhmageContract.Ohmlets.OHMLET_NAME, name);
        values.put(OhmageContract.Ohmlets.OHMLET_DESCRIPTION, description);
        values.put(OhmageContract.Ohmlets.OHMLET_SURVEYS, saver.gson().toJson(surveys));
        values.put(OhmageContract.Ohmlets.OHMLET_STREAMS, saver.gson().toJson(streams));
        values.put(OhmageContract.Ohmlets.OHMLET_MEMBERS, saver.gson().toJson(people));
        if (privacyState != null)
            values.put(OhmageContract.Ohmlets.OHMLET_PRIVACY_STATE, privacyState.ordinal());
        return values;
    }

    @Override
    public void read(ContentProviderReader reader, Cursor cursor) {
        ohmletId = cursor.getString(0);
        name = cursor.getString(1);
        description = cursor.getString(2);
        streams = reader.gson().fromJson(cursor.getString(3), Streams.class);
        surveys = reader.gson().fromJson(cursor.getString(4), Surveys.class);
        people = reader.gson().fromJson(cursor.getString(5), Member.List.class);
        privacyState = PrivacyState.values()[cursor.getInt(6)];
    }
}
