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

  // used to specify the OP-local id inside Service elements of type signon
  private static final String LOCAL_ID_TAG = "LocalID";

  // injected fetcher
  private final HttpFetcher httpFetcher;

  @Inject
  public LegacyXrdsResolver(HttpFetcher httpFetcher) {
    this.httpFetcher = httpFetcher;
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
  public List<DiscoveryInformation> findOpEndpointsForSite(IdpIdentifier site,
      URI siteXrdsUri) throws DiscoveryException {
    return resolveXrds(getXrd(siteXrdsUri), siteXrdsUri,
        DiscoveryInformation.OPENID2_OP, site);
  }

  /**
   * Returns a list of discovery info objects from a user's XRDS document.
   * The document's canonical ID is expected to be equal to the claimedID of
   * the user.
   * @param claimedId the claimedId of the user
   * @param userXrdsUri the URI from which to download the user's XRDS document.
   */
  public List<DiscoveryInformation> findOpEndpointsForUser(
      UrlIdentifier claimedId, URI userXrdsUri) throws DiscoveryException {

      return resolveXrds(getXrd(userXrdsUri), userXrdsUri,
          DiscoveryInformation.OPENID2, claimedId);
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
  public List<DiscoveryInformation> findOpEndpointsForUserThroughSiteXrd(
      UrlIdentifier claimedId, URI siteXrdsUri) throws DiscoveryException {

    // We're given the XRDS for the site of the claimedID.
    // Perform mapping to extract user's XRDS location.
    URI userXrdsUri = mapClaimedIdToUserXrdsUri(getXrd(siteXrdsUri), claimedId);

    // now that we have the user XRDS URI, we fetch the XRDS
    // and return the list of OP endpoints found in there.
    return resolveXrds(getXrd(userXrdsUri), userXrdsUri,
        DiscoveryInformation.OPENID2, claimedId);
  }

  /**
   * Looks for a URITemplate in the XRD, and applies the claimed id to it in
   * order to generate the user's XRDS endpoint.
   *
   * @throws DiscoveryException
   */
  /* visible for testing */
  URI mapClaimedIdToUserXrdsUri(XRD xrd, UrlIdentifier claimedId)
      throws DiscoveryException {

    // find the <Service> element with type '.../describedby'
    Service service = getServiceForType(xrd, URI_TEMPLATE_TYPE);
    if (service == null) {
      throw new DiscoveryException("could not find service of type " +
          URI_TEMPLATE_TYPE + " in XRDS at location " +
          claimedId.getIdentifier());
    }

    // find the <URITemplate> tag inside the <Service> element
    String uriTemplate = getTagValue(service, URI_TEMPLATE_TAG);
    if (uriTemplate == null) {
      throw new DiscoveryException("missing " + URI_TEMPLATE_TAG + " in " +
          "service specification in XRDS at location " +
          claimedId.getIdentifier());
    }

    // now, apply the mapping:
    UriTemplate template = new UriTemplate(uriTemplate);
    return template.map(URI.create(claimedId.getIdentifier()));
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
   * @param source the source from which we've fetched the XRD (for error
   *   reporting purposes)
   * @param version the type of <Service> element we're looking for, can be
   *   either http://specs.openid.net/auth/2.0/signon or
   *   http://specs.openid.net/auth/2.0/server
   * @param id the identifier (UrlIdentifier for claimedId, or IdPIdentifier
   *   for site discovery)
   * @return a list of discovery info objects.
   * @throws DiscoveryException
   */
  private List<DiscoveryInformation> resolveXrds(XRD xrd, URI source,
      String version, Identifier id) throws DiscoveryException {

    List<Service> services = getServicesForType(xrd, version);

    if (services == null) {
      throw new DiscoveryException("could not find <Service> of type " +
          version + " in XRDS for " + source.toASCIIString());
    }

    List<DiscoveryInformation> result =
        Lists.newArrayListWithCapacity(services.size());

    for (Service service : services) {
      try {
        if (version.equals(DiscoveryInformation.OPENID2)) {
          // look for LocalID and use claimedID, if given.
          result.add(createDiscoveryInfoForSignon(service, id));
        } else if (version.equals(DiscoveryInformation.OPENID2_OP)) {
          // for site discovery, just return the URI
          result.add(createDiscoveryInfoForServer(service));
        } else {
          throw new DiscoveryException("unkown OpenID version : " + version);
        }
      } catch (MalformedURLException e) {
        logger.log(Level.WARNING, "found malformed URL in discovery document " +
            "at " + source.toASCIIString(), e);
        continue;
      }
    }

    return result;
  }

  /**
   * Returns a simple {@link DiscoveryInformation} object pointing to an
   * OP endpoint.
   * @param service The <Service> element that has the OP endpoint information.
   * @return a {@link DiscoveryInformation} object.
   * @throws DiscoveryException
   * @throws MalformedURLException
   */
  private DiscoveryInformation createDiscoveryInfoForServer(Service service)
      throws DiscoveryException, MalformedURLException {
    return new DiscoveryInformation(service.getURIAt(0).getURI().toURL());
  }

  /**
   * Returns a {@link DiscoveryInformation} object pointing to an
   * OP endpoint, and possibly containing other information such as the
   * claimedId and the OP-local id.
   * @param service The <Service> element that has the OP endpoint information.
   * @return a {@link DiscoveryInformation} object.
   * @throws DiscoveryException
   * @throws MalformedURLException
   */
  private DiscoveryInformation createDiscoveryInfoForSignon(Service service,
      Identifier claimedId) throws DiscoveryException, MalformedURLException {

    // could be null
    String localId = getTagValue(service, LOCAL_ID_TAG);

    return new DiscoveryInformation(service.getURIAt(0).getURI().toURL(),
        claimedId, localId, DiscoveryInformation.OPENID2);
  }

  /**
   * Fetches an XRD from a URI and returns it, or throws if the XRD can't be
   * fetched/found.
   * @param uri from where to fetch the XRDS.
   * @throws DiscoveryException
   */
  private XRD getXrd(URI uri) throws DiscoveryException {
    XRD result;
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
   * @throws FetchException
   */
  private XRD fetchXrd(URI uri) throws FetchException {

    FetchRequest request = FetchRequest.createGetRequest(uri);

    XRDS xrds;
    try {
      FetchResponse response = httpFetcher.fetch(request);

      Document document = XmlUtil.getDocument(response.getContentAsStream());

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

    return xrds.getFinalXRD();
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
}
