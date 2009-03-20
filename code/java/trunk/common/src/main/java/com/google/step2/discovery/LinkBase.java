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
 * Super class for Link and Link-Pattern. This is basically a wrapper around
 * {@link LinkValue}, which is a class that can parse the link values (i.e.,
 * the stuff to the right of the colon in Link: <...>; ...).
 */
public abstract class LinkBase {

  private final LinkValue value;
  private final String prefix;

  protected LinkBase(String input, String prefix) throws LinkSyntaxException {
    this.value = getLinkValue(input, prefix);

    // we keep track of the prefix only for our equals() and hashCode() methods
    this.prefix = prefix.toLowerCase();
  }

  /**
   * Returns the LinkValue that this instance represents. LinkValues know
   * about rel-types, mime-types, and general link parameters.
   */
  public LinkValue getLinkValue() {
    return value;
  }

  /**
   * Returns the list of rel-types specified in this link or link-pattern.
   */
  public RelTypes getRelationships() {
    return getLinkValue().getRelationships();
  }

  /**
   * Returns the mime-type of this link or link-pattern.
   */
  public String getMimeType() {
    return getLinkValue().getMimeType();
  }

  /**
   * Returns the value of a link parameter. For example, in
   *
   * Link: <http://example.com/path>; foo=bar
   *
   * this method, when called with name = "foo", would return "bar".
   *
   * @param name the name of the parameter.
   */
  public String getParamater(String name) {
    return getLinkValue().getParameter(name);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LinkBase other = (LinkBase) obj;
    if (prefix == null) {
      if (other.prefix != null) return false;
    } else if (!prefix.equals(other.prefix)) return false;
    if (value == null) {
      if (other.value != null) return false;
    } else if (!value.equals(other.value)) return false;
    return true;
  }

  /**
   * Helper method to parse the link value (i.e., the stuff to the right of
   * the colon), given the whole line, and the prefix (i.e., the stuff to the
   * left of the colon).
   * @param input the complete line of text
   * @param prefix the stuff to the left of the colon (e.g. "Link" or
   *   "Link-Pattern")
   * @return the parsed link-value
   * @throws LinkSyntaxException if the stuff to the right of the colon couldn't
   *   be parsed, or if the line of text doesn't follow the pattern
   *   prefix: link-value.
   */
  private static LinkValue getLinkValue(String input, String prefix)
      throws LinkSyntaxException {

    int colon = input.indexOf(':');

    if (colon < 0) {
      throw new LinkSyntaxException("missing colon:" + input);
    }

    String left = input.substring(0, colon).trim();
    String right = input.substring(colon + 1).trim();

    if (!left.equalsIgnoreCase(prefix)) {
      throw new LinkSyntaxException("missing " + prefix + " prefix: " + input);
    }

    return LinkValue.fromString(right);
  }
}
