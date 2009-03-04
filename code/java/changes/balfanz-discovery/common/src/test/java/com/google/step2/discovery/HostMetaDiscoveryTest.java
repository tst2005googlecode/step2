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

import com.google.step2.discovery.HostMetaDiscovery.SiteHostMetaDiscovery;
import com.google.step2.discovery.HostMetaDiscovery.UserHostMetaDiscovery;

import junit.framework.TestCase;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.UrlIdentifier;

public class HostMetaDiscoveryTest extends TestCase {


  private UserHostMetaDiscovery userDiscoverer;
  private SiteHostMetaDiscovery siteDiscoverer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    userDiscoverer = new UserHostMetaDiscovery();
    siteDiscoverer = new SiteHostMetaDiscovery();
  }

  public void testFindOpEndpointsForSite() throws Exception {

    IdpIdentifier host = new IdpIdentifier("host");

    HostMeta hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.openid.net/auth/2.0/server\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    DiscoveryInformation result =
        siteDiscoverer.findOpEndpointInHostMeta(hostMeta, host);

    assertEquals("http://foo.com/bar2", result.getOPEndpoint().toString());
    assertEquals(DiscoveryInformation.OPENID2_OP, result.getVersion());

    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link: <http://foo.com/bar2>; rel=\"openid http://specs.openid.net/auth/2.0/server\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    result = siteDiscoverer.findOpEndpointInHostMeta(hostMeta, host);

    assertEquals("http://foo.com/bar2", result.getOPEndpoint().toString());
    assertEquals(DiscoveryInformation.OPENID2_OP, result.getVersion());

    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"foobar\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.foo.net/auth/2.5/xrd");

    result = siteDiscoverer.findOpEndpointInHostMeta(hostMeta, host);

    assertNull(result);

    // if it's not a URL we get an exception:
    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link: <bar2>; rel=\"http://specs.openid.net/auth/2.0/server\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    try {
      siteDiscoverer.findOpEndpointInHostMeta(hostMeta, host);
      fail("expected exception, but didn't get it");
    } catch(DiscoveryException e) {
      // expected
    }


    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link: <foo:bar2>; rel=\"http://specs.openid.net/auth/2.0/server\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    try {
      siteDiscoverer.findOpEndpointInHostMeta(hostMeta, host);
      fail("expected exception, but didn't get it");
    } catch(DiscoveryException e) {
      // expected
    }


  }

  public void testFindOpEndpointsForUser() throws Exception {

    UrlIdentifier claimedId = new UrlIdentifier("http://example.com/bob");

    HostMeta hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link-Pattern: <http://foo.com/bar2?uri={%uri}>; rel=\"http://specs.openid.net/auth/2.0/signon\"",
        "Link-Pattern: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    DiscoveryInformation result =
        userDiscoverer.findOpEndpointInHostMeta(hostMeta, claimedId);

    assertEquals("http://foo.com/bar2?uri=http%3A%2F%2Fexample.com%2Fbob",
        result.getOPEndpoint().toString());
    assertEquals(claimedId.toString(), result.getClaimedIdentifier().toString());
    assertEquals(DiscoveryInformation.OPENID2, result.getVersion());
    assertNull(result.getDelegateIdentifier());

    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link-Pattern: <http://foo.com/bar2?uri={%uri}>; " +
            "rel=\"foobar http://specs.openid.net/auth/2.0/signon\"; " +
            "local-id=http://foo.com/bob",
        "Link-Pattern: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    result = userDiscoverer.findOpEndpointInHostMeta(hostMeta, claimedId);

    assertEquals("http://foo.com/bar2?uri=http%3A%2F%2Fexample.com%2Fbob",
        result.getOPEndpoint().toString());
    assertEquals(claimedId.toString(), result.getClaimedIdentifier().toString());
    assertEquals(DiscoveryInformation.OPENID2, result.getVersion());
    assertEquals("http://foo.com/bob", result.getDelegateIdentifier());

    hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=\"foobar\"",
        "Link: <http://foo.com/bar3>; rel=http://specs.foo.net/auth/2.5/xrd");

    result = userDiscoverer.findOpEndpointInHostMeta(hostMeta, claimedId);

    assertNull(result);

    // if it's not a URL we get an exception:
    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link-Pattern: <uri={%uri}>; " +
            "rel=\"foobar http://specs.openid.net/auth/2.0/signon\"; " +
            "local-id=http://foo.com/bob",
        "Link-Pattern: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    try {
      userDiscoverer.findOpEndpointInHostMeta(hostMeta, claimedId);
      fail("expected exception, but didn't get it");
    } catch(DiscoveryException e) {
      // expected
    }


    hostMeta = getHostMeta(
        "Link-Pattern: <http://foo.com/bar1>; rel=\"describedby\"",
        "Link-Pattern: <foo:uri={%uri}>; " +
            "rel=\"foobar http://specs.openid.net/auth/2.0/signon\"; " +
            "local-id=http://foo.com/bob",
        "Link-Pattern: <http://foo.com/bar3>; rel=http://specs.openid.net/auth/2.5/xrd");

    try {
      userDiscoverer.findOpEndpointInHostMeta(hostMeta, claimedId);
      fail("expected exception, but didn't get it");
    } catch(DiscoveryException e) {
      // expected
    }
  }

  static HostMeta getHostMeta(String... links) {
    StringBuilder hostMeta = new StringBuilder();

    for(String link : links) {
      hostMeta.append(link).append("\n");
    }
    return HostMeta.parseFromBytes(hostMeta.toString().getBytes());
  }
}
