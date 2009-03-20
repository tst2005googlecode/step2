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
package com.google.step2.discovery;

/**
 * Represents a link-pattern header
 */
public class LinkPattern extends LinkBase {

  public static LinkPattern fromString(String input) throws LinkSyntaxException {
    return new LinkPattern(input);
  }

  public LinkPattern(String input) throws LinkSyntaxException {
    super(input, "Link-Pattern");
  }

  /**
   * Returns the string between the '<' and '>' in a Link-Pattern. Since that
   * is not necessarily a syntactically correct URI, we're returning it as a
   * string.
   */
  public String getUriPattern() {
    return getLinkValue().getUriString();
  }
}
