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

import static org.easymock.EasyMock.expect;

import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.UrlIdentifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class LegacyXrdsResolverTest extends TestCase {

  private static String SITE_XRD =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns:openid=\"http://openid.net/" +
        "xmlns/1.0\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>balfanz.net</CanonicalID>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/server</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "</Service>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://www.iana.org/assignments/relation/describedby</Type>\n" +
    "<MediaType>application/xdrs+xml</MediaType>\n" +
    "<URITemplate>https://www.google.com/accounts/o8/user-xrds?uri={%uri}" +
        "</URITemplate>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";

  private IMocksControl control;
  private Discovery yadis;
  private HttpFetcher fetcher;
  private LegacyXrdsResolver xrdResolver;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    yadis = control.createMock(Discovery.class);
    fetcher = control.createMock(HttpFetcher.class);
    xrdResolver = new LegacyXrdsResolver(yadis, fetcher);
  }

  public void testFindOpEndpointsForSite() throws Exception {
    IdpIdentifier host = new IdpIdentifier("host");
    URI siteXrdsUri = URI.create("http://example.com/xrds");
    List<DiscoveryInformation> infos = Arrays.asList(
        // a server info
        new DiscoveryInformation(new URL("http://example.com/op1")),
        // a signon info
        new DiscoveryInformation(
            new URL("http://example.com/op2"),
            new UrlIdentifier("http://bob.com")));

    expect(yadis.discover(siteXrdsUri.toString())).andReturn(infos);

    control.replay();
    List<DiscoveryInformation> result =
        xrdResolver.findOpEndpoints(host, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    DiscoveryInformation info = result.get(0);
    assertEquals("http://example.com/op1", info.getOPEndpoint().toString());
  }

  public void testFindOpEndpointsForUser() throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://bob.com/id");
    URI siteXrdsUri = URI.create("http://example.com/xrds");
    List<DiscoveryInformation> infos = Arrays.asList(
        // a server info
        new DiscoveryInformation(new URL("http://example.com/op1")),
        // a signon info
        new DiscoveryInformation(new URL("http://example.com/op2"), user));

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(SITE_XRD));
    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
      + URLEncoder.encode(user.getIdentifier(), "UTF-8");
    expect(yadis.discover(userXrdsUri)).andReturn(infos);

    control.replay();
    List<DiscoveryInformation> result =
        xrdResolver.findOpEndpoints(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    DiscoveryInformation info = result.get(0);
    assertEquals("http://example.com/op2", info.getOPEndpoint().toString());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
  }

  private static class FakeResponse implements FetchResponse {

    private final String content;
    private final int status;

    public FakeResponse(String content) {
      this.status = 200;
      this.content = content;
    }

    public InputStream getContentAsStream() {
      return new ByteArrayInputStream(content.getBytes());
    }

    public byte[] getContentAsBytes() {
      return content.getBytes();
    }

    public int getStatusCode() {
      return status;
    }
  }
}
