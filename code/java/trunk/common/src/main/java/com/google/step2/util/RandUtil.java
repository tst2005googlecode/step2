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

import java.math.BigInteger;
import java.security.SecureRandom;


/**
 * Simple class to get random bytes, which can be replaced during testing.
 */
public class RandUtil {

  public static final SecureRandom secureRandom = new SecureRandom();

  /**
   * Generates a given number of random bytes, and then returns them as a
   * hexadecimal number.
   * @param entropyBytes how many random bytes we want generated
   * @return a string, which the hexadecimal representation of the bytes.
   */
  public static final String getRandomString(int entropyBytes) {
    byte[] bytes = new byte[entropyBytes];
    secureRandom.nextBytes(bytes);
    return new BigInteger(bytes).toString(16);
  }
}
