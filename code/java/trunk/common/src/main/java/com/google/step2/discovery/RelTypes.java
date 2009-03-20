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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that represents a collection of rel-types. (A link can have
 * multiple rel-types attached to it.)
 */
public class RelTypes {

  private final Set<RelType> types;

  public static RelTypes setOf(RelType...types) {
    return setOf(Arrays.asList(types));
  }

  public static RelTypes setOf(Collection<RelType> types) {
    return new RelTypes(types);
  }

  private RelTypes(Collection<RelType> types) {
    this.types = new HashSet<RelType>(types);
  }

  public boolean contains(RelType type) {
    return types.contains(type);
  }

  public boolean containsAll(RelTypes other) {
    return types.containsAll(other.types);
  }

  public int size() {
    return types.size();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((types == null) ? 0 : types.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    RelTypes other = (RelTypes) obj;
    if (types == null) {
      if (other.types != null) return false;
    } else if (!types.equals(other.types)) return false;
    return true;
  }
}
