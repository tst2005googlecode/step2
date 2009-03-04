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

import java.io.ByteArrayInputStream;
import java.util.Collection;

public class HostMetaTest extends TestCase {

  private String hostMetaStr =
    "# stuff that should parse: \n" +
    "Link: <http://foo.com/>; rel=\"fo;o\"\n" +
    "Link: <http://foo.com/>;rel=foo;type=bar  \n" +
    "Link: <http://foo.com/>;rel=\"foo bar\";type=bar\n" +
    "Link: <http://foo.com/>;rel=foo ;type=\"bar foo\"\n" +
    "Link: <http://foo.com/>  \n" +
    "Link: <http://foo.com/bar;foo>\n" +
    "Link: <http://foo.com/bar>; type=zxy>trw;rel=foo  \n" +
    "Link-Pattern: <http://foo.com/bar;foo>; rel=describedby\n" +
    "Link-Pattern: <http://foo.com/bar;foo>; localid=\"foobar\"\n" +
    "  \n" + "\n" +
    " # stuff that should not parse: \n" +
    "Lunk: <http://foo.com/>; rel=\"foo\" \n" +
    "Link: <http://foo.com/>; rel=fo;o\n" +
    "Link: <http://foo.com/>; rel=foo;tupe=ba r\n" +
    "Link-Pattern: <http://foo.com/bar;foo; rel=describedby\n" +
    "Lunk-Pattern: <http://foo.com/bar;foo>; rel=describedby\n" +
    "Link-Pattern: <http://foo.com/bar;foo>; localid=foo bar\"\n";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testParseFromStream() throws Exception {
    ByteArrayInputStream hostMetaStream =
        new ByteArrayInputStream(hostMetaStr.getBytes());
    HostMeta hostMeta = HostMeta.parseFromStream(hostMetaStream);

    assertEquals(7, hostMeta.getLinks().size());
    assertEquals(2, hostMeta.getLinkPatterns().size());
  }

  public void testParseFromBytes() throws Exception {
    HostMeta hostMeta = HostMeta.parseFromBytes(hostMetaStr.getBytes());
    Collection<Link> links = hostMeta.getLinks();

    assertEquals(7, links.size());
  }
}
