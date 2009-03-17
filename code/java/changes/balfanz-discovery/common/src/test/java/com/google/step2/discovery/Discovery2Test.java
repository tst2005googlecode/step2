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

import static com.google.step2.discovery.XrdLocationSelectorTest.getHostMeta;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.Lists;
import com.google.step2.discovery.Discovery2.FallbackDiscovery;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Discovery2Test extends TestCase {

  private IMocksControl control;
  private HostMetaFetcher hostMetafetcher;
  private Discovery2 discovery;
  private XrdDiscoveryResolver xrdResolver;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    hostMetafetcher = control.createMock(HostMetaFetcher.class);

    xrdResolver = control.createMock(XrdDiscoveryResolver.class);

    discovery = new Discovery2(hostMetafetcher, xrdResolver);
  }

  public void testDiscoverOpEndpointsForSite() throws Exception {

    IdpIdentifier host = new IdpIdentifier("host");
    List<SecureDiscoveryInformation> infos = Lists.newArrayList();

    HostMeta hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml");

    expect(xrdResolver.getDiscoveryDocumentType())
        .andStubReturn("application/xrds+xml");
    expect(hostMetafetcher.getHostMeta(host.getIdentifier()))
        .andReturn(hostMeta);
    expect(xrdResolver.findOpEndpointsForSite(host, URI.create("http://foo.com/bar2")))
        .andReturn(infos);

    control.replay();

    List<SecureDiscoveryInformation> result = discovery.discoverOpEndpointsForSite(host);

    control.verify();

    assertSame(infos, result);
  }

  public void testTryHostMetaBasedClaimedIdDiscovery() throws Exception {

    UrlIdentifier user = new UrlIdentifier("http://bob.com/myid");
    List<SecureDiscoveryInformation> infos = Lists.newArrayList();

    HostMeta hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link-Pattern: <http://foo.com/bar2?uri={%uri}>; " +
            "rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; " +
            "type=application/xrds+xml");

    expect(xrdResolver.getDiscoveryDocumentType())
        .andStubReturn("application/xrds+xml");
    expect(hostMetafetcher.getHostMeta("bob.com"))
        .andReturn(hostMeta);
    expect(xrdResolver.findOpEndpointsForUser(user,
        URI.create("http://foo.com/bar2?uri=" + URLEncoder.encode(user.getIdentifier(), "UTF-8"))))
        .andReturn(infos);

    control.replay();

    List<SecureDiscoveryInformation> result = discovery.tryHostMetaBasedDiscoveryForUser(user);

    control.verify();

    assertSame(infos, result);
  }

  public void testTryHostMetaBasedClaimedIdDiscovery_siteXrd() throws Exception {

    UrlIdentifier user = new UrlIdentifier("http://bob.com/myid");
    List<SecureDiscoveryInformation> infos = Lists.newArrayList();

    HostMeta hostMeta = getHostMeta(
        "Link: <http://foo.com/bar1>; rel=foobar; type=application/xrds+xml",
        "Link: <http://foo.com/bar2>; rel=\"http://specs.foo.net/auth/2.5/xrd-op describedby\"; type=application/xrds+xml");

    expect(xrdResolver.getDiscoveryDocumentType())
        .andStubReturn("application/xrds+xml");
    expect(hostMetafetcher.getHostMeta("bob.com"))
        .andReturn(hostMeta);
    expect(xrdResolver.findOpEndpointsForUserThroughSiteXrd(user, URI.create("http://foo.com/bar2")))
        .andReturn(infos);

    control.replay();

    List<SecureDiscoveryInformation> result = discovery.tryHostMetaBasedDiscoveryForUser(user);

    control.verify();

    assertSame(infos, result);
  }

  public void testFallbackDiscovery_newStyle() throws Exception {

    IdpIdentifier host = new IdpIdentifier("host");
    List<SecureDiscoveryInformation> infos = Lists.newArrayList();
    infos.add(new SecureDiscoveryInformation(new URL("http://foo.com")));

    FallbackDiscovery<Identifier> mockFallback =
        control.createMock(ForwardingFallbackDiscoverer.class);

    ForwardingFallbackDiscoverer fallback =
        new ForwardingFallbackDiscoverer(mockFallback);

    expect(mockFallback.newStyleDiscovery(host)).andReturn(infos);

    control.replay();

    List<SecureDiscoveryInformation> result = fallback.get(host);

    control.verify();

    assertSame(infos, result);
  }

  public void testFallbackDiscovery_oldStyle() throws Exception {
    IdpIdentifier host = new IdpIdentifier("host");
    UrlIdentifier legacy = new UrlIdentifier("http://legacy.com");

    List<DiscoveryInformation> infos = new ArrayList<DiscoveryInformation>();
    infos.add(new DiscoveryInformation(new URL("http://foo.com")));

    FallbackDiscovery<Identifier> mockFallback =
        control.createMock(ForwardingFallbackDiscoverer.class);

    ForwardingFallbackDiscoverer fallback =
        new ForwardingFallbackDiscoverer(mockFallback);

    expect(mockFallback.newStyleDiscovery(host)).andReturn(null);
    expect(mockFallback.getLegacyIdentifier(host)).andReturn(legacy);
    expect(mockFallback.oldStyleDiscovery(legacy)).andReturn(infos);

    control.replay();

    List<SecureDiscoveryInformation> result = fallback.get(host);

    control.verify();

    assertEquals(Discovery2.convertToNewDiscoveryInfo(infos), result);
  }


  private class ForwardingFallbackDiscoverer
      extends FallbackDiscovery<Identifier> {

    private FallbackDiscovery<Identifier> delegate;

    public ForwardingFallbackDiscoverer(FallbackDiscovery<Identifier> delegate) {
      discovery.super();
      this.delegate = delegate;
    }

    @Override
    public Identifier getLegacyIdentifier(Identifier id) throws DiscoveryException {
      return delegate.getLegacyIdentifier(id);
    }

    @Override
    public List<SecureDiscoveryInformation> newStyleDiscovery(Identifier id)
        throws DiscoveryException {
      return delegate.newStyleDiscovery(id);
    }

    @Override
    List<DiscoveryInformation> oldStyleDiscovery(Identifier id)
        throws DiscoveryException {
      return delegate.oldStyleDiscovery(id);
    }
  }
}
