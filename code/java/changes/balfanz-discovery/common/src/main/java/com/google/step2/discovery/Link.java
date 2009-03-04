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

import java.net.URI;

/**
 * Represents a link header
 */
public class Link extends LinkBase {

  public static Link fromString(String input) throws LinkSyntaxException {
    return new Link(input);
  }

  private final URI uri;

  public Link(String input) throws LinkSyntaxException {
    super(input, "Link");

    // this particular subclass of LinkBase knows that the URI string
    // returned by the superclass is, in fact, a URI and not some sort
    // of URI template pattern, so we're parsing it right here and now
    // as a URI.
    try {
      uri = URI.create(getLinkValue().getUriString());
    } catch (IllegalArgumentException e) {
      throw new LinkSyntaxException(e);
    }
  }

  /**
   * Returns the URI that the Link points to.
   */
  public URI getUri() {
    return uri;
  }
}
