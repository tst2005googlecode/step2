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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;
import com.google.step2.util.XmlUtil;
import com.google.step2.xmlsimplesign.CertValidator;
import com.google.step2.xmlsimplesign.VerificationResult;
import com.google.step2.xmlsimplesign.Verifier;
import com.google.step2.xmlsimplesign.XmlSimpleSignException;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;
import org.openxri.xml.Service;
import org.openxri.xml.XRD;
import org.openxri.xml.XRDS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements XRDS-based discovery.
 */
public class LegacyXrdsResolver implements XrdDiscoveryResolver {

  private static final Logger logger =
      Logger.getLogger(LegacyXrdsResolver.class.getName());

  // the type of meta-data document this resolver understands
  private static final String XRDS_TYPE = "application/xrds+xml";

  // specifies a link that points to a document that includes meta data.
  private static final String URI_TEMPLATE_TYPE =
    "http://www.iana.org/assignments/relation/describedby";

  // used to generate URIs pointing to user-specific XRDS documents
  private static final String URI_TEMPLATE_TAG = "URITemplate";

  // used to delegate to new signer in next XRDS document
  private static final String NEXT_AUTHORITY_TAG = "NextAuthority";

  // used to specify the OP-local id inside Service elements of type signon
  private static final String LOCAL_ID_TAG = "LocalID";

  // injected fetcher
  private final HttpFetcher httpFetcher;

  // injected XRD signature verifier
  private final Verifier verifier;

  // the object that will validate the signing cert, i.e., decide whether
  // the signing cert belongs to an authority appropriate for the given XRD
  private final CertValidator certValidator;

  @Inject
  public LegacyXrdsResolver(HttpFetcher httpFetcher, Verifier verifier,
      CertValidator validator) {
    this.httpFetcher = httpFetcher;
    this.verifier = verifier;
    this.certValidator = validator;
  }

  public String getDiscoveryDocumentType() {
    return XRDS_TYPE;
  }

  /**
   * Finds OP endpoints in a site's XRDS.
   * @param siteXrdsUri the URI from which to load the site's XRDS.
   * @return a list of discovery infos.
   * @throws DiscoveryException
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForSite(
      IdpIdentifier site, URI siteXrdsUri) throws DiscoveryException {
    return resolveXrds(getXrd(siteXrdsUri), DiscoveryInformation.OPENID2_OP,
        site, null);
  }

  /**
   * Returns a list of discovery info objects from a user's XRDS document.
   * The document's canonical ID is expected to be equal to the claimedID of
   * the user.
   * @param claimedId the claimedId of the user
   * @param userXrdsUri the URI from which to download the user's XRDS document.
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForUser(
      UrlIdentifier claimedId, URI userXrdsUri) throws DiscoveryException {

      return resolveXrds(getXrd(userXrdsUri), DiscoveryInformation.OPENID2,
          claimedId, null);
  }

  /**
   * Returns a list of discovery info objects from a user's XRDS document, but
   * starts discovery at the site's XRDS document. The site's XRDS document
   * (whose canonical ID is expected to match the host in the claimed ID) is
   * expected to contain URITemplate elements which will point to the user's
   * XRDS document. The latter document's canonical ID is expected to be equal
   * to the claimedID of the user.
   * @param claimedId the claimedId of the user
   * @param siteXrdsUri the URI from which to download the user's XRDS document.
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForUserThroughSiteXrd(
      UrlIdentifier claimedId, URI siteXrdsUri) throws DiscoveryException {

    // We're given the XRDS for the site of the claimedID.
    // Perform mapping to extract user's XRDS location.
    NextXrdLocation userXrdsLocation =
        mapClaimedIdToUserXrdsUri(getXrd(siteXrdsUri), claimedId);

    // now that we have the user XRDS URI, we fetch the XRDS
    // and return the list of OP endpoints found in there.
    return resolveXrds(getXrd(userXrdsLocation.getUri()),
        DiscoveryInformation.OPENID2,
        claimedId,
        userXrdsLocation.getNextAuthority());
  }

  /**
   * Looks for a URITemplate in the XRD, and applies the claimed id to it in
   * order to generate the user's XRDS endpoint.
   *
   * @param siteXrd the XRD for the site (host) identified in the claimedId
   *
   * @return A {@link NextXrdLocation}, which is a struct containing the URI
   *   obtained by mapping the claimedId onto the URITemplate found in the XRD,
   *   and also a String identifying the next authority expected to sign the
   *   XRD that the URI points to. This authority string might be null, which
   *   means that the XRD that the URI points to should be signed by an
   *   authority that matches the claimedId.
   *
   * @throws DiscoveryException
   */
  /* visible for testing */
  NextXrdLocation mapClaimedIdToUserXrdsUri(XrdRepresentations siteXrd,
      UrlIdentifier claimedId) throws DiscoveryException {

    // extract the host from the claimed id - this is the canonicalID
    // we expect in the site's XRD
    IdpIdentifier host = new IdpIdentifier(claimedId.getUrl().getHost());

    // find the <Service> element with type '.../describedby'
    Service service = getServiceForType(siteXrd.getXrd(), URI_TEMPLATE_TYPE);
    if (service == null) {
      throw new DiscoveryException("could not find service of type " +
          URI_TEMPLATE_TYPE + " in XRDS at location " +
          claimedId.getIdentifier());
    }

    // is there a NextAuthority? We only trust the next authority element
    // if the document is properly signed.
    String nextAuthority = checkSecurity(siteXrd, host, null)
        ? getTagValue(service, NEXT_AUTHORITY_TAG)  // might still be null
        : null;                                     // must be null if unsigned

    // find the <URITemplate> tag inside the <Service> element
    String uriTemplate = getTagValue(service, URI_TEMPLATE_TAG);
    if (uriTemplate == null) {
      throw new DiscoveryException("missing " + URI_TEMPLATE_TAG + " in " +
          "service specification in XRDS at location " +
          claimedId.getIdentifier());
    }

    // now, apply the mapping:
    UriTemplate template = new UriTemplate(uriTemplate);
    URI newUri = template.map(URI.create(claimedId.getIdentifier()));

    return new NextXrdLocation(newUri, nextAuthority);
  }

  /**
   * Returns the first value of a tag inside a Service element.
   * @param service the Service element inside which we're looking for a tag.
   * @param tagName the name of the tag
   * @return the value of the tag, or null if no such tag exists.
   */
  private String getTagValue(Service service, String tagName) {
    @SuppressWarnings("unchecked")
    Vector<Element> tags = service.getOtherTagValues(tagName);
    if (tags == null || tags.size() == 0) {
      return null;
    }

    // we're just looking at the first tag
    return tags.get(0).getTextContent();
  }

  /**
   * Finds OP-endpoints in an XRDS document.
   * @param xrd The XRD in which we're looking for OP endpoints.
   * @param version the type of <Service> element we're looking for, can be
   *   either http://specs.openid.net/auth/2.0/signon or
   *   http://specs.openid.net/auth/2.0/server
   * @param id the identifier (UrlIdentifier for claimedId, or IdPIdentifier
   *   for site discovery)
   * @param authority who we expect to be signing this XRD. If this is null,
   *   then the XRD must be signed by an authority that matches the
   *   canonicalID in the document.
   * @return a list of discovery info objects.
   * @throws DiscoveryException
   */
  private List<SecureDiscoveryInformation> resolveXrds(XrdRepresentations xrd,
      String version, Identifier id, String authority)
      throws DiscoveryException {

    boolean isSecure = checkSecurity(xrd, id, authority);

    List<Service> services = getServicesForType(xrd.getXrd(), version);

    if (services == null) {
      throw new DiscoveryException("could not find <Service> of type " +
          version + " in XRDS for " + xrd.getSource());
    }

    List<SecureDiscoveryInformation> result =
        Lists.newArrayListWithCapacity(services.size());

    for (Service service : services) {
      try {
        if (version.equals(DiscoveryInformation.OPENID2)) {
          // look for LocalID and use claimedID, if given.
          result.add(createDiscoveryInfoForSignon(service, id, isSecure));
        } else if (version.equals(DiscoveryInformation.OPENID2_OP)) {
          // for site discovery, just return the URI
          result.add(createDiscoveryInfoForServer(service, isSecure));
        } else {
          throw new DiscoveryException("unkown OpenID version : " + version);
        }
      } catch (MalformedURLException e) {
        logger.log(Level.WARNING, "found malformed URL in discovery document " +
            "at " + xrd.getSource(), e);
        continue;
      }
    }

    return result;
  }

  /**
   * Checks whether the XRD is properly signed.
   * @param xrd the XRD in question.
   * @param id the id that we expect this XRD to be about.
   * @param authority the authority that we expect this document to have signed.
   *   If null, the document should be signed by an authority matching the
   *   CanonicalId.
   * @return true if the signature could be validated, false otherwise
   */
  private boolean checkSecurity(XrdRepresentations xrd, Identifier id,
      String authority) {

    // first, we make sure that the canonicalID in this XRD matches
    // the given identifier
    String canonicalId = getCanonicalId(xrd.getXrd());
    if (canonicalId == null) {
      logger.warning("XRD from " + xrd.getSource() +
          "did not have canonical Id");
      return false;
    }

    if (!canonicalId.equals(id.getIdentifier())) {
      logger.warning("Canonical ID " + canonicalId + " in XRD from " +
          xrd.getSource() + " did not equal identifier " +
          id.getIdentifier());
      return false;
    }

    // now, check the signature:
    VerificationResult verificatioResult;
    try {
      verificatioResult = verifier.verify(xrd.getDocument(), xrd.getSignature());
    } catch (XmlSimpleSignException e) {
      logger.log(Level.WARNING, "signature on XRD from " + xrd.getSource() +
          "did not verify", e);
      return false;
    }

    // finally, validate the signing cert (make sure it belongs to the authority
    // that is supposed to have signed this XRD). If we're not given an
    // authority, the XRD should be signed by the entity identified in the
    // canonical id.
    authority = (authority == null) ? canonicalId : authority;
    return certValidator.matches(verificatioResult.getCerts().get(0), authority);
  }

  /**
   * Returns CanonicalId of this document. There should be exactly one
   * CanonicalId in the document for us to consider the document securel.
   * @param xrd
   */
  private String getCanonicalId(XRD xrd) {
    if (xrd.getNumCanonicalids() != 1) {
      return null;
    }
    return xrd.getCanonicalidAt(0).getValue();
  }

  /**
   * Returns a simple {@link SecureDiscoveryInformation} object pointing to an
   * OP endpoint.
   * @param service The <Service> element that has the OP endpoint information.
   * @param isSecure whether to mark the {@link SecureDiscoveryInformation}
   *   object as secure.
   * @return a {@link SecureDiscoveryInformation} object.
   * @throws DiscoveryException
   * @throws MalformedURLException
   */
  private SecureDiscoveryInformation createDiscoveryInfoForServer(
      Service service, boolean isSecure) throws DiscoveryException, MalformedURLException {
    SecureDiscoveryInformation result =
        new SecureDiscoveryInformation(service.getURIAt(0).getURI().toURL());
    result.setSecure(isSecure);
    return result;
  }

  /**
   * Returns a {@link SecureDiscoveryInformation} object pointing to an
   * OP endpoint, and possibly containing other information such as the
   * claimedId and the OP-local id.
   * @param service The <Service> element that has the OP endpoint information.
   * @param claimedId the claimedId we currently performing discovery on.
   * @param isSecure whether to mark the {@link SecureDiscoveryInformation}
   *   object as secure.
   * @return a {@link SecureDiscoveryInformation} object.
   * @throws DiscoveryException
   * @throws MalformedURLException
   */
  private SecureDiscoveryInformation createDiscoveryInfoForSignon(
      Service service, Identifier claimedId, boolean isSecure)
      throws DiscoveryException, MalformedURLException {

    // could be null
    String localId = getTagValue(service, LOCAL_ID_TAG);

    SecureDiscoveryInformation result = new SecureDiscoveryInformation(
        service.getURIAt(0).getURI().toURL(),
        claimedId,
        localId,
        DiscoveryInformation.OPENID2);

    result.setSecure(isSecure);
    return result;
  }

  /**
   * Fetches an XRD from a URI and returns it, or throws if the XRD can't be
   * fetched/found.
   * @param uri from where to fetch the XRDS.
   * @throws DiscoveryException
   */
  private XrdRepresentations getXrd(URI uri) throws DiscoveryException {
    XrdRepresentations result;
    try {
      result = fetchXrd(uri);
    } catch (FetchException e) {
      throw new DiscoveryException("could not fetch XRDS from "
          + uri.toASCIIString(), e);
    }
    if (result == null) {
      throw new DiscoveryException("XRDS at " + uri.toASCIIString() + " did " +
          "not contain an XRD");
    }
    return result;
  }

  /**
   * Fetches an OpenID 2.0-style XRDS document and returns the "final" XRD
   * from it.
   *
   * @return an {@link XrdRepresentations} object, which not only contains the
   *   parsed XRD, but also the document as a byte array, the URI from which
   *   the XRD was fetched, and the Signature that we might have see in the
   *   HTTP response's Signature header.
   *
   * @throws FetchException
   */
  private XrdRepresentations fetchXrd(URI uri) throws FetchException {

    FetchRequest request = FetchRequest.createGetRequest(uri);

    XRDS xrds;
    byte[] documentBytes;
    String signature;

    try {
      FetchResponse response = httpFetcher.fetch(request);

      documentBytes = response.getContentAsBytes();
      signature = response.getFirstHeader("Signature"); // could be null

      Document document =
          XmlUtil.getDocument(new ByteArrayInputStream(documentBytes));

      xrds = new XRDS(document.getDocumentElement(), false);

    } catch (ParserConfigurationException e) {
      throw new FetchException(e);
    } catch (SAXException e) {
      throw new FetchException(e);
    } catch (IOException e) {
      throw new FetchException(e);
    } catch (URISyntaxException e) {
      throw new FetchException(e);
    } catch (ParseException e) {
      throw new FetchException(e);
    }

    return new XrdRepresentations(xrds.getFinalXRD(), uri.toASCIIString(),
        documentBytes, signature);
  }

  /**
   * Returns highest-priority service for given type from an XRD
   */
  private Service getServiceForType(XRD xrd, String type) {

    @SuppressWarnings("unchecked")
    Vector<Service> services = xrd.getServicesByType(type);

    if (services == null || services.size() == 0) {
      return null;
    }

    Service result = services.get(0);
    int priority = result.getPriority();

    // see whether there are services with higher (i.e. smaller) priority
    for (Service service : services) {
      if (service.getPriority() < priority) {
        priority = service.getPriority();
        result = service;
      }
    }

    return result;
  }

  /**
   * Returns services (highest-priority first) for given type from an XRD
   */
  private List<Service> getServicesForType(XRD xrd, String type) {

    @SuppressWarnings("unchecked")
    Vector<Service> services = xrd.getServicesByType(type);

    if (services == null || services.size() == 0) {
      return null;
    }

    Collections.sort(services, new Comparator<Service>() {
      public int compare(Service o1, Service o2) {
        return o2.getPriority().compareTo(o1.getPriority());
      }
    });

    return services;
  }

  /**
   * Helper class that bundles the location of the next XRD document in the
   * discovery chain, together with the authority that should sign that next
   * XRD document.
   */
  private static class NextXrdLocation {

    private final URI uri;
    private final String nextAuthority;

    public NextXrdLocation(URI uri, String nextAuthority) {
      this.uri = uri;
      this.nextAuthority = nextAuthority;
    }

    public URI getUri() {
      return uri;
    }

    public String getNextAuthority() {
      return nextAuthority;
    }
  }

  /**
   * Helper class that hold two different representations of the XRD: the
   * parsed version (useful for extracting information from it), and the
   * raw bytes (useful for verifying the signature). Also holds the value
   * of the Signature: header, if it was present when fetching the XRD, and
   * the location (source) from which the the XRD was fetched.
   */
  private static class XrdRepresentations {

    private final XRD xrd;
    private final byte[] document;
    private final String source;
    private final String signature;

    public XrdRepresentations(XRD xrd, String source, byte[] document, String signature) {
      this.xrd = xrd;
      this.source = source;
      this.document = document;
      this.signature = signature;
    }

    public XRD getXrd() {
      return xrd;
    }

    public byte[] getDocument() {
      return document;
    }

    public String getSignature() {
      return signature;
    }

    public String getSource() {
      return source;
    }
  }
}
