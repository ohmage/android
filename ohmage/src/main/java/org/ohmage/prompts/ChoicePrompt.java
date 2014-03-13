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

package org.ohmage.prompts;

import java.util.ArrayList;

/**
 * Created by cketcham on 1/24/14.
 */
public class ChoicePrompt<T,V> extends AnswerablePrompt<T> {

    public ChoiceArray<V> choices;

    public static class ChoiceArray<T> extends ArrayList<KLVPair<T>> {

        public int indexOfValue(T object) {
            KLVPair<T> tmp = new KLVPair<T>();
            tmp.value = object;
            return super.indexOf(tmp);
        }
    }

    public static class KLVPair<T> implements CharSequence {
        public String text;

        public T value;

        @Override public int length() {
            String str = text;
            if (str == null) {
                if (value == null) return 0;
                str = value.toString();
            }
            return str.length();
        }

        @Override public char charAt(int index) {
            String str = text;
            if (str == null) {
                if (value == null) throw new IndexOutOfBoundsException();
                str = value.toString();
            }
            return str.charAt(index);
        }

        @Override public CharSequence subSequence(int start, int end) {
            String str = text;
            if (str == null) {
                if (value == null) throw new IndexOutOfBoundsException();
                str = value.toString();
            }
            return str.subSequence(start, end);
        }

        @Override public String toString() {
            String str = text;
            if (str == null) {
                if (value == null) return "";
                str = value.toString();
            }
            return str;
        }

        @Override public boolean equals(Object o) {
            return (o instanceof KLVPair) && (value == null) ? ((KLVPair) o).value == null :
                    value.equals(((KLVPair) o).value);
        }
    }
}
