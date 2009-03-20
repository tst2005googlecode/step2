/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.google.step2.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;

/**
 * Base64 encoding and decoding.
 */
public class EncodingUtil {

  public static Charset UTF8 = Charset.forName("UTF-8");

  public static String getUtf8String(byte[] data) {
    return UTF8.decode(ByteBuffer.wrap(data)).toString();
  }

  public static byte[] getUtf8Bytes(String s) {
    if (s == null) {
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    ByteBuffer bb = UTF8.encode(s);
    return ArrayUtils.subarray(bb.array(), 0, bb.limit());
  }

  public static String encodeBase64(byte[] bytes) {
    return EncodingUtil.getUtf8String(Base64.encodeBase64(bytes, false));
  }

  public static byte[] decodeBase64(String b64) {
    return Base64.decodeBase64(getUtf8Bytes(b64));
  }

  public static byte[] decodeBase64(byte[] b64) {
    return Base64.decodeBase64(b64);
  }
}
