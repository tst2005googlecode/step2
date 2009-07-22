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
import org.openid4java.discovery.UrlIdentifier;
import java.net.URI;

public class XrdLocationSelectorTest extends TestCase {

  private XrdLocationSelector selector;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    selector = new XrdLocationSelector();
  }

  public void testFindXrdUriForOp_forSite() throws Exception {

    String mimeType = "application/xrds+xml";
    IdpIdentifier host = new IdpIdentifier("host.com");

    // first, what if it's not in there at all
    HostMeta hostMeta = getHostMeta(
        "Link: <http://foo.com/bar>; rel=\"describedby\"",  // missing mime-type
        "Link: <http://foo.com/bar2>; rel=http://specs.openid.net/auth/2.0/server");

    assertNull(selector.findSiteXrdUriForOp(hostMeta, mimeType));


    // if all possible types are present, it should pick the most specific one
    URI correctUri = URI.create("http://foo.com/bar2");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType() +
            " describedby\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType() +
            "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar3");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType() +
            " describedby\"; type=somethingelse",
        "Link: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType() +
            " describedby\"; type=somethingelse",
        "Link: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=somethingelse");

    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar3>; rel=\"describedby http://specs.foo.net/auth/2.5/xrd\";  type=application/xrds+xml");
    // this one
    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar3>; rel=\"foobar http://specs.foo.net/auth/2.5/xrd\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // should pick up bar2, since it includes valid type "describedby"
    correctUri = URI.create("http://foo.com/bar2");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"foobar describedby\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar3>; rel=\"" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findSiteXrdUriForOp(hostMeta, mimeType));

    // should return null
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op foobar\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar3>; rel=" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + ";  type=application/xrds+xml");

    assertNull(selector.findSiteXrdUriForOp(hostMeta, mimeType));
  }

  public void testFindXrdUriForOp_forUser() throws Exception {

    String mimeType = "application/xrds+xml";
    UrlIdentifier user = new UrlIdentifier("http://host.com/bob");

    // first, what if it's not in there at all
    HostMeta hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar>; rel=\"describedby\"",  // missing mime-type
        "Link-Pattern: <http://foo.com/bar2>; rel=http://specs.openid.net/auth/2.0/server");

    assertNull(selector.findUserXrdUriForOp(hostMeta, mimeType, user));


    // if all possible types are present, it should pick the most specific one
    URI correctUri = URI.create("http://foo.com/bar2");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType()
            + " describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar3");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType() +
            " describedby\"; type=somethingelse",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType() +
            "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"" + XrdLocationSelector.REL_OPENID_OP_XRD.getRelationshipType() +
            " describedby\"; type=somethingelse",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"describedby " + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=somethingelse");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"describedby http://specs.foo.net/auth/2.5/xrd\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // pick the most specific one
    correctUri = URI.create("http://foo.com/bar1");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=describedby; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"foobar http://specs.foo.net/auth/2.5/xrd\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // should pick up bar2, since it includes valid type "describedby"
    correctUri = URI.create("http://foo.com/bar2");
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"foobar describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3>; rel=\"" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // should return null
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op foobar\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3>; rel=" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + ";  type=application/xrds+xml");

    assertNull(selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // if there is a link-pattern, it should have precendence
    correctUri = URI.create("http://foo.com/bar3?uri=http%3A%2F%2Fhost.com%2Fbob");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"foobar describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3?uri={%uri}>; rel=\"foobar describedby\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar4>; rel=\"" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertEquals(correctUri, selector.findUserXrdUriForOp(hostMeta, mimeType, user));

    // if the link-pattern is no good, result should be null
    correctUri = URI.create("http://foo.com/bar2");
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"foobar describedby\"; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar3?uri={%uri}>; rel=\"foobar\"; type=application/xrds+xml",
        "Link: <http://foo.com/bar4>; rel=\"" + XrdLocationSelector.REL_OPENID_XRD.getRelationshipType()
            + "\";  type=application/xrds+xml");

    assertNull(selector.findUserXrdUriForOp(hostMeta, mimeType, user));
  }

  static HostMeta getHostMeta(String... links) {
    StringBuilder hostMeta = new StringBuilder();

    for(String link : links) {
      hostMeta.append(link).append("\n");
    }
    return HostMeta.parseFromBytes(hostMeta.toString().getBytes());
  }
}
