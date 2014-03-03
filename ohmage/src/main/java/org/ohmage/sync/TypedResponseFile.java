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

package org.ohmage.sync;

/**
 * Created by cketcham on 12/11/13.
 */

import java.io.File;

import retrofit.mime.TypedFile;

/**
 * Represents a file for a response
 */
public class TypedResponseFile extends TypedFile {

    private final String mId;

    /**
     * Constructs a new typed file for a response.
     *
     * @param id
     * @param file
     * @throws NullPointerException if file is null
     */
    public TypedResponseFile(String id, File file) {
        super("application/octet-stream", file);
        mId = id;
    }

    @Override public String fileName() {
        return mId;
    }
}