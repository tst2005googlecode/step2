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
 * Class that represents rel-types for links.
 */
public class RelType {

  private static final URI BASE_URI =
      URI.create("http://www.iana.org/assignments/relation/");

  private final URI uri;

  public RelType(URI uri) {
    this.uri = BASE_URI.resolve(uri);
  }

  public RelType(String uri) {
    this(URI.create(uri));
  }

  public String getRelationshipType() {
    return uri.toASCIIString();
  }

  @Override
  public String toString() {
    return "[rel: " + getRelationshipType() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    RelType other = (RelType) obj;
    if (uri == null) {
      if (other.uri != null) return false;
    } else if (!uri.equals(other.uri)) return false;
    return true;
  }
}
