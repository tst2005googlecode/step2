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

public class LinkTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testFromString() throws Exception {

    ////////////////////////////////////////////////////////////////////////////
    // things that should parse
    Link l = Link.fromString("Link: <http://foo.com/> ; rel=\"foo\"");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/>; rel=\"foo\"");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/>; rel=foo");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString(" Link: <http://foo.com/>; rel=foo ");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/bar?a=b&c=d#frag%2F>; rel=foo");
    assertEquals("http://foo.com/bar?a=b&c=d#frag%2F", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/>; rel=\"foo bar\"");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(2, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));
    assertTrue(l.getRelationships().contains(new RelType("bar")));

    l = Link.fromString("Link: <http://foo.com/>; rel=\"foo bar\"; rel=baz");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(3, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));
    assertTrue(l.getRelationships().contains(new RelType("bar")));
    assertTrue(l.getRelationships().contains(new RelType("baz")));

    l = Link.fromString("Link: <http://foo.com/>" +
        ";rel=\"describedby http://specs.openid.net/auth/2.0\"");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(2, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("describedby")));
    assertTrue(l.getRelationships().contains(
        new RelType("http://specs.openid.net/auth/2.0")));

    l = Link.fromString("Link: <http://foo.com/>; rel=\"fo;o\"");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("fo;o")));

    l = Link.fromString("Link: <http://foo.com/>;rel=foo;type=bar");
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/>;rel=\"foo bar\";type=bar");
    assertTrue(l.getRelationships().contains(new RelType("foo")));
    assertTrue(l.getRelationships().contains(new RelType("bar")));

    l = Link.fromString("Link: <http://foo.com/>;rel=foo ;type=\"bar foo\"");
    assertTrue(l.getRelationships().contains(new RelType("foo")));

    l = Link.fromString("Link: <http://foo.com/>");
    assertEquals("http://foo.com/", l.getUri().toString());
    assertEquals(0, l.getRelationships().size());

    l = Link.fromString("Link: <http://foo.com/bar;foo>");
    assertEquals("http://foo.com/bar;foo", l.getUri().toString());

    l = Link.fromString("Link: <http://foo.com/bar>; type=zxy>trw;rel=foo");
    assertEquals("http://foo.com/bar", l.getUri().toString());
    assertEquals(1, l.getRelationships().size());
    assertTrue(l.getRelationships().contains(new RelType("foo")));
    assertEquals("zxy>trw", l.getMimeType());

    l = Link.fromString("Link: <http://foo.com/bar>; type=zxy>trw;foo=bar");
    assertEquals("http://foo.com/bar", l.getUri().toString());
    assertEquals("bar", l.getParamater("foo"));

    ////////////////////////////////////////////////////////////////////////////
    // things that should not parse
    try {
      Link.fromString("Lunk: <http://foo.com/>; rel=\"foo\"");
      fail("expected syntax exception, but didn't get it");
    } catch (LinkSyntaxException e) {
      // expected
    }

    try {
      Link.fromString("Link: <http://foo.com/>; rel=fo;o");
      fail("expected syntax exception, but didn't get it");
    } catch (LinkSyntaxException e) {
      // expected
    }

    try {
      Link.fromString("Link: <http://foo.com/>; rel=foo;tupe=foo b=ar");
      fail("expected syntax exception, but didn't get it");
    } catch (LinkSyntaxException e) {
      // expected
    }

    try {
      Link.fromString("Link: <http://foo.com/>;");
      fail("expected syntax exception, but didn't get it");
    } catch (LinkSyntaxException e) {
      // expected
    }

    try {
      Link.fromString("Link: http://foo.com/; rel=describedby");
      fail("expected syntax exception, but didn't get it");
    } catch (LinkSyntaxException e) {
      // expected
    }
  }
}
