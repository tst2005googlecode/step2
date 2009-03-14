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

import com.google.inject.Inject;
import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;
import com.google.step2.util.XmlUtil;

import org.openid4java.discovery.Discovery;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements XRDS-based discovery. In this file, we have an abstract
 * superclass, and two different subclasses implementing the XRDS discovery
 * for UrlIdentifiers (for user discovery) and IdPIdentifiers (for site
 * discovery).
 */
public class LegacyXrdsResolver extends AbstractXrdDiscoveryResolver {

  // the type of meta-data document this resolver understands
  static private final String XRDS_TYPE = "application/xrds+xml";

  // specifies a link that points to a document that includes meta data.
  private static final String URI_TEMPLATE_TYPE =
    "http://www.iana.org/assignments/relation/describedby";

  // used to generate URIs pointing to user-specific XRDS documents
  private static final String URI_TEMPLATE_TAG = "URITemplate";

  // old-style yadis discovery we use to scan through the XRDS
  protected final Discovery legacyDiscovery;

  // injected fetcher
  protected final HttpFetcher httpFetcher;

  @Inject
  public LegacyXrdsResolver(Discovery discovery, HttpFetcher httpFetcher) {
    this.legacyDiscovery = discovery;
    this.httpFetcher = httpFetcher;
  }

  @Override
  public String getDiscoveryDocumentType() {
    return XRDS_TYPE;
  }

  /**
   * XRDS-based user discovery works as follows: First, we fetch the site-wide
   * XRDS from the URI provided to us (which was obtained from a /host-meta).
   *
   * Then, we look for URITemplates in the XRDS, which will point us to the
   * user's XRDS. Finally, we look for OP endpoints in the user's XRDS.
   */
  @Override
  protected List<DiscoveryInformation> findOpEndpointsForUser(
      UrlIdentifier claimedId, URI siteXrdsUri) throws DiscoveryException {
    // fetch site-wide XRD
    XRD xrd;
    try {
      xrd = fetchXrd(siteXrdsUri);
    } catch (FetchException e) {
      throw new DiscoveryException("could not fetch XRDS from " + siteXrdsUri,
          e);
    }

    if (xrd == null) {
      throw new DiscoveryException("XRDS at " + siteXrdsUri + " did not " +
          "contain any XRD.");
    }

    // perform mapping to extract user's XRDS location.
    URI userXrdsuri = mapClaimedIdToUserXrdsUri(xrd, claimedId);

    // now that we have the user XRDS URI, we can use the
    // parse it and return the list of OP endpoints found in there.
    return findSignonEndpointsInUserXrds(claimedId, userXrdsuri);
  }


  /**
   * Finds OP endpoints in a user's XRDS.
   * @param claimedId the claimed id given to us. This claimedId should be
   *   included in the discovery infos returned.
   * @param userXrdsUri the URI from which to load the user's XRDS.
   * @return a list of discovery infos.
   * @throws DiscoveryException
   */
  private List<DiscoveryInformation> findSignonEndpointsInUserXrds(
      UrlIdentifier claimedId, URI userXrdsUri) throws DiscoveryException {

    // the legacy yadis discoverer will fill <uri> as the claimed id, since
    // that is where it's downloading the XRDS from. We know, however, that
    // we're performing discovery on <claimedId>, so we will return that in
    // in the discovery info objects.
    return filterByVersion(getLegacyDiscoveries(userXrdsUri),
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
    @SuppressWarnings("unchecked")
    Vector<Element> templates = service.getOtherTagValues(URI_TEMPLATE_TAG);
    if (templates == null || templates.size() == 0) {
      throw new DiscoveryException("missing " + URI_TEMPLATE_TAG + " in " +
          "service specification in XRDS at location " +
          claimedId.getIdentifier());
    }

    // we're just looking at the first URITemplate:
    Element element = templates.get(0);

    // now, apply the mapping:
    UriTemplate template = new UriTemplate(element.getTextContent());
    return template.map(URI.create(claimedId.getIdentifier()));
  }

  /**
   * Finds OP endpoints in a site's XRDS.
   * @param siteXrdsUri the URI from which to load the site's XRDS.
   * @return a list of discovery infos.
   * @throws DiscoveryException
   */
  @Override
  protected List<DiscoveryInformation> findOpEndpointsForSite(
      URI siteXrdsUri) throws DiscoveryException {
    return filterByVersion(getLegacyDiscoveries(siteXrdsUri),
        DiscoveryInformation.OPENID2_OP, null);
  }

  @SuppressWarnings("unchecked")
  private List<DiscoveryInformation> getLegacyDiscoveries(URI uri)
      throws DiscoveryException {
    return legacyDiscovery.discover(uri.toString());
  }

  /**
   * Fetches an OpenID 2.0-styel XRDS document and returns the "final" XRD
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
   * Filters a list of discovery infos to include only those infos that
   * match a certain version. Also, optionally, changes the claimed id's
   * in those discovery infos to that provided.
   * @param list unfiltered list
   * @param version the version by which to filter
   * @param id if non-null, change the claimed id to this. If null, don't
   *   change the claimed id.
   * @return the filtered list
   */
  private List<DiscoveryInformation> filterByVersion(
      List<DiscoveryInformation> list, String version, Identifier id)
      throws DiscoveryException {

    if (list == null) {
      return Collections.emptyList();
    }

    ArrayList<DiscoveryInformation> result =
          new ArrayList<DiscoveryInformation>();

    for (DiscoveryInformation info : list) {
      if (version.equals(info.getVersion())) {
        if (id == null) {

          result.add(info);

        } else {

          // we were told to change the claimed_id in the discovery info
          result.add(new DiscoveryInformation(
              info.getOPEndpoint(),
              id,
              info.getDelegateIdentifier(),
              version));
        }
      }
    }
    return result;
  }
}
