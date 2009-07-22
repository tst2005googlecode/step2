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

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;

import com.google.common.collect.ImmutableList;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;
import com.google.step2.xmlsimplesign.CertConstantUtil;
import com.google.step2.xmlsimplesign.CertValidator;
import com.google.step2.xmlsimplesign.VerificationResult;
import com.google.step2.xmlsimplesign.Verifier;
import com.google.step2.xmlsimplesign.XmlSimpleSignException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.openid4java.discovery.UrlIdentifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

public class LegacyXrdsResolverTest extends TestCase {

  private static String SITE_XRD =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>balfanz.net</CanonicalID>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/server</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "</Service>\n" +
    "<Service priority=\"0\" xmlns:openid=\"http://namespace.google.com/openid/xmlns\">\n" +
    "<Type>http://www.iana.org/assignments/relation/describedby</Type>\n" +
    "<MediaType>application/xdrs+xml</MediaType>\n" +
    "<openid:URITemplate>https://www.google.com/accounts/o8/user-xrds?uri={%uri}" +
        "</openid:URITemplate>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";

  private static String SITE_XRD_NEXT_AUTHORITY =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>balfanz.net</CanonicalID>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/server</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "</Service>\n" +
    "<Service priority=\"0\" xmlns:openid=\"http://namespace.google.com/openid/xmlns\">\n" +
    "<Type>http://www.iana.org/assignments/relation/describedby</Type>\n" +
    "<MediaType>application/xdrs+xml</MediaType>\n" +
    "<openid:URITemplate>https://www.google.com/accounts/o8/user-xrds?uri={%uri}" +
        "</openid:URITemplate>\n" +
    "<openid:NextAuthority>www.google.com</openid:NextAuthority>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";


  private static String USER_XRD =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>http://balfanz.net/openid?id=12345</CanonicalID>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/signon</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";

  private static String USER_XRD_WITH_LOCAL_ID =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>http://balfanz.net/openid?id=12345</CanonicalID>\n" +
    "<Service priority=\"0\" xmlns:openid=\"http://namespace.google.com/openid/xmlns\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/signon</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "<openid:LocalID>12345</openid:LocalID>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";

  private IMocksControl control;
  private HttpFetcher fetcher;
  private LegacyXrdsResolver xrdResolver;
  private Verifier verifier;
  private CertValidator validator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    fetcher = control.createMock(HttpFetcher.class);
    verifier = control.createMock(Verifier.class);
    validator = control.createMock(CertValidator.class);
    xrdResolver = new LegacyXrdsResolver(fetcher, verifier, validator);
  }

  public void testFindOpEndpointsForSite() throws Exception {
    IdpIdentifier host = new IdpIdentifier("balfanz.net");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(verifier.verify(isA(byte[].class), (String) isNull()))
        .andStubThrow(new XmlSimpleSignException("no signature"));
    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(SITE_XRD));

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForSite(host, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertFalse(info.isSecure());
  }

  public void testFindOpEndpointsForUser_direct() throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(verifier.verify(isA(byte[].class), (String) isNull()))
        .andStubThrow(new XmlSimpleSignException("no signature"));
    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(USER_XRD));

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForUser(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertFalse(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertNull(info.getDelegateIdentifier());
  }

  public void testFindOpEndpointsForUser_directWithLocalId() throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(verifier.verify(isA(byte[].class), (String) isNull()))
        .andStubThrow(new XmlSimpleSignException("no signature"));
    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(USER_XRD_WITH_LOCAL_ID));

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForUser(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertFalse(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertEquals("12345", info.getDelegateIdentifier());
  }

  public void testFindOpEndpointsForUser_throughSiteXrds() throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(verifier.verify(isA(byte[].class), (String) isNull()))
        .andStubThrow(new XmlSimpleSignException("no signature"));
    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(SITE_XRD));

    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
        + URLEncoder.encode(user.getIdentifier(), "UTF-8");

    FetchRequest nextRequest = FetchRequest.createGetRequest(URI.create(userXrdsUri));

    expect(fetcher.fetch(nextRequest)).andReturn(new FakeResponse(USER_XRD));

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForUserThroughSiteXrd(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertFalse(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertNull(info.getDelegateIdentifier());
  }

  public void testFindOpEndpointsForUser_throughSiteXrds_secure()
      throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);
    VerificationResult verification = new VerificationResult(
        ImmutableList.of(
            CertConstantUtil.SERVER_PUB_CERT,
            CertConstantUtil.INTERMEDIATE_PUB_CERT));

    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(SITE_XRD));
    expect(verifier.verify(aryEq(SITE_XRD.getBytes()), (String) isNull()))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT, "balfanz.net"))
        .andReturn(Boolean.TRUE);

    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
        + URLEncoder.encode(user.getIdentifier(), "UTF-8");

    FetchRequest nextRequest = FetchRequest.createGetRequest(
        URI.create(userXrdsUri));

    expect(fetcher.fetch(nextRequest)).andReturn(new FakeResponse(USER_XRD));
    expect(verifier.verify(aryEq(USER_XRD.getBytes()), (String) isNull()))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT,
        "http://balfanz.net/openid?id=12345"))
        .andReturn(Boolean.TRUE);

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForUserThroughSiteXrd(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertTrue(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertNull(info.getDelegateIdentifier());
  }

  public void testFindOpEndpointsForUser_throughSiteXrds_withNextAuthority()
      throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);
    VerificationResult verification = new VerificationResult(
        ImmutableList.of(
            CertConstantUtil.SERVER_PUB_CERT,
            CertConstantUtil.INTERMEDIATE_PUB_CERT));

    expect(fetcher.fetch(httpRequest))
        .andReturn(new FakeResponse(SITE_XRD_NEXT_AUTHORITY));
    expect(verifier.verify(aryEq(SITE_XRD_NEXT_AUTHORITY.getBytes()),
        (String) isNull()))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT, "balfanz.net"))
        .andReturn(Boolean.TRUE);

    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
        + URLEncoder.encode(user.getIdentifier(), "UTF-8");

    FetchRequest nextRequest = FetchRequest.createGetRequest(
        URI.create(userXrdsUri));

    expect(fetcher.fetch(nextRequest)).andReturn(new FakeResponse(USER_XRD));
    expect(verifier.verify(aryEq(USER_XRD.getBytes()), (String) isNull()))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT,
        "www.google.com"))
        .andReturn(Boolean.TRUE);

    control.replay();
    List<SecureDiscoveryInformation> result =
      xrdResolver.findOpEndpointsForUserThroughSiteXrd(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertTrue(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertNull(info.getDelegateIdentifier());
  }

  public void testFindOpEndpointsForUser_throughSiteXrds_SignatureInHeader()
      throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://balfanz.net/openid?id=12345");
    URI siteXrdsUri = URI.create("http://example.com/xrds");

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);
    VerificationResult verification = new VerificationResult(
        ImmutableList.of(
            CertConstantUtil.SERVER_PUB_CERT,
            CertConstantUtil.INTERMEDIATE_PUB_CERT));
    FakeResponse siteResponse = new FakeResponse(SITE_XRD);
    siteResponse.setSignature("siteSig");

    FakeResponse userResponse = new FakeResponse(USER_XRD);
    userResponse.setSignature("userSig");

    expect(fetcher.fetch(httpRequest)).andReturn(siteResponse);
    expect(verifier.verify(aryEq(SITE_XRD.getBytes()), eq("siteSig")))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT, "balfanz.net"))
        .andReturn(Boolean.TRUE);

    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
        + URLEncoder.encode(user.getIdentifier(), "UTF-8");

    FetchRequest nextRequest = FetchRequest.createGetRequest(
        URI.create(userXrdsUri));

    expect(fetcher.fetch(nextRequest)).andReturn(userResponse);
    expect(verifier.verify(aryEq(USER_XRD.getBytes()), eq("userSig")))
        .andReturn(verification);
    expect(validator.matches(CertConstantUtil.SERVER_PUB_CERT,
        "http://balfanz.net/openid?id=12345"))
        .andReturn(Boolean.TRUE);

    control.replay();
    List<SecureDiscoveryInformation> result =
        xrdResolver.findOpEndpointsForUserThroughSiteXrd(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    SecureDiscoveryInformation info = result.get(0);
    assertEquals("https://www.google.com/a/balfanz.net/o8/ud?be=o8",
        info.getOPEndpoint().toString());
    assertTrue(info.isSecure());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
    assertNull(info.getDelegateIdentifier());
  }

  private static class FakeResponse implements FetchResponse {

    private final String content;
    private final int status;
    private String signature;

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

    public String getFirstHeader(String name) {
      if ("Signature".equals(name)) {
        return signature;
      } else {
        return null;
      }
    }

    public void setSignature(String s) {
      this.signature = s;
    }
  }
}
