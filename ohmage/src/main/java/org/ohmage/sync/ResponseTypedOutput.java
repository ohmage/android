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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;

import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

/**
 * Uploads a response and its associated media
 */
public class ResponseTypedOutput implements TypedOutput {

    private final MultipartTypedOutput mMulitpartType;

    private static final String sDataFormatter = "[{\"data\":%s,\"meta_data\":%s}]";

    /**
     * Just a helper class for deserializing response file info
     */
    public static class ResponseFiles extends HashMap<String, String> {

        public Set<String> getIds() {
            return keySet();
        }

        public File getFile(String id) {
            return new File(get(id));
        }
    }

    public ResponseTypedOutput(String data, String metaData, ResponseFiles files) {
        mMulitpartType = new MultipartTypedOutput();
        mMulitpartType.addPart("data", new TypedByteArray("application/json",
                String.format(sDataFormatter, data, metaData).getBytes()));
        for (String id : files.getIds()) {
            mMulitpartType.addPart("media",
                    new TypedResponseFile(files.getFile(id).getName(), files.getFile(id)));
        }
    }

    @Override public String fileName() {
        return mMulitpartType.fileName();
    }

    @Override public String mimeType() {
        return mMulitpartType.mimeType();
    }

    @Override public long length() {
        return mMulitpartType.length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
        mMulitpartType.writeTo(out);
    }
}