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

/**
 * Created by cketcham on 12/19/13.
 */
public class Prompt {
    public String text;
    public String value;

    public boolean skippable = true;

    public Prompt(String text) {
        this.text = text;
    }

//    public static class PromptTypeAdapter implements TypeAdapterFactory {
//
//        @Override
//        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
//            if(type.getRawType() != Prompt.class)
//                return null;
//
//            final TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);
//
//            return new TypeAdapter<T>() {
//                @Override
//                public void write(JsonWriter out, T value) throws IOException {
//                    delegateAdapter.write(out, value);
//                }
//
//                @Override
//                public T read(JsonReader in) throws IOException {
//                    JsonElement tree = elementAdapter.read(in);
//                    afterRead(tree);
//                    return delegate.fromJsonTree(tree);
//                }
//            };
//        }
//    }
//
//    public static class PromptDeserializer implements JsonDeserializer<Prompt> {
//        public ValueOrError<V> deserialize(JsonElement json, Type typeOfT,
//                                           JsonDeserializationContext context) {
//            JsonObject object = json.getAsJsonObject();
//            String type = object.get("survey_item_type").getAsString();
//
//            if (error != null) {
//                ErrorEntity entity = context.deserialize(error, ErrorEntity.class);
//                return ValueOrError.<V>error(entity);
//            } else {
//                Type valueType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
//                V value = (V) context.deserialize(json, valueType);
//                return ValueOrError.value(value);
//            }
//        }
//    }

}
