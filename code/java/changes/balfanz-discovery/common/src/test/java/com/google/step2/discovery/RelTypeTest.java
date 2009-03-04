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

import junit.framework.TestCase;

import java.net.URI;

public class RelTypeTest extends TestCase {

  public void testConstructor() throws Exception {
    URI foobar = URI.create("foobar");
    assertEquals("http://www.iana.org/assignments/relation/foobar",
        new RelType(foobar).getRelationshipType());
    assertEquals("http://www.iana.org/assignments/relation/foobar",
        new RelType("foobar").getRelationshipType());

    URI mailto = URI.create("mailto:franz@banz.com");
    assertEquals("mailto:franz@banz.com",
        new RelType(mailto).getRelationshipType());
    assertEquals("mailto:franz@banz.com",
        new RelType("mailto:franz@banz.com").getRelationshipType());


    URI absolute = URI.create("http://foo.com/bar");
    assertEquals("http://foo.com/bar",
        new RelType(absolute).getRelationshipType());
    assertEquals("http://foo.com/bar",
        new RelType("http://foo.com/bar").getRelationshipType());
  }
}
