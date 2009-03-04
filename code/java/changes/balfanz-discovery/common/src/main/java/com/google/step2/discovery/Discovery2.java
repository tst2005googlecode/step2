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

import static com.google.step2.discovery.RelTypes.setOf;

import com.google.inject.Inject;

import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implements Next-Generation OpenID discovery, based on Link-headers,
 * link-elements, and host-meta. There are two different discovery
 * operations available:
 *
 * (1) discover the OP endpoint(s) for a "site", and
 * (2) discover the OP endpoint(s) for an OpenID (which is the id of a user)
 *
 * Case (1) is for when users merely indicate the IdP to the RP, case (2) is
 * for when users actually submit their OpenID to the RP.
 *
 * The strategy for case (1) is as follows:
 *
 * - find the host-meta file for the site.
 * - check whether the host-meta file points directly to an openid endpoint.
 * - if not, find a link in the host-meta that points to an XRD(S) metadata
 *   document for the site
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * The strategy for case (2) is as follows:
 *
 * - Try link-header strategy (2a)
 * - If that fails, try link-element strategy (2b)
 * - If that fails, try host-meta strategy (2c)
 *
 * Strategy (2a) works as follow:
 *
 * - check whether a link HTTP header in the HTTP response for the OpenID
 *   points directly to the openid endpoint
 * - if not, find a link in the HTTP headers in the HTTP response that points
 *   to an XRD(S) metadata document for the OpenID's URI
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * Strategy (2b) works as follow:
 *
 * - check whether an HTML link element in the document returned from
 *   the OpenID URI points directly to the openid endpoint
 * - if not, find an HTML link element in the document returned from
 *   the OpenID URI that points to an XRD(S) metadata document for the
 *   OpenID's URI
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * Strategy (2c) works as follows:
 *
 * - find the host-meta file for the host identified in the OpenID URL.
 * - in the host-meta file, check whether a Link-Pattern in the host-meta
 *   points to an XRD(S) for the OpenID URL.
 * - if not, find a Link in the host-meta that points to an XRD(S) metadata
 *   document for the site
 * - follow the URITemplate link in the site XRD(S) to find the XRD(S) for the
 *   the OpenID URL.
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 *
 * For backwards compatibility, we also provide a generic discover() method,
 * which decides whether to use site-discovery or user-id-discovery based on the
 * type of the provided identifier. This allows us to use this class inside the
 * openid4java library.
 *
 * When calling this class through the legacy generic discover() method, we also
 * employ a "fallback" strategy, i.e. we first try the strategy described above,
 * and then fall back to OpenID 2.0-style discovery, if the strategy above
 * doesn't yield any results.
 */
public class Discovery2 extends Discovery {

  // specifies a link that points directly to an OpenID endpoint (for site
  // discovery)
  public static final RelType REL_OP_ENDPOINT_SERVER =
      new RelType("http://specs.openid.net/auth/2.0/server");

  // specifies a link that points directly to an OpenID endpoint (for user id
  // discovery)
  public static final RelType REL_OP_ENDPOINT_SIGNON =
    new RelType("http://specs.openid.net/auth/2.0/signon");

  // specifies a link that points to an XRD(S) document that includes meta
  // data about OpenID OPs. Whether the document is old-style XRDS or new-style
  // XRD depends on the (MIME) type specified in the Link.
  public static final RelType REL_OPENID_OP_XRD =
    new RelType("http://specs.openid.net/auth/2.5/xrd-op");

  // specifies a link that points to an XRD(S) document that includes meta
  // data about OpenID RPs. Whether the document is old-style XRDS or new-style
  // XRD depends on the (MIME) type specified in the Link.
  public static final RelType REL_OPENID_RP_XRD =
    new RelType("http://specs.openid.net/auth/2.5/xrd-rp");

  // specifies a link that points to an XRD(S) document that includes meta
  // data about OpenID. Whether the document is old-style XRDS or new-style
  // XRD depends on the (MIME) type specified in the Link.
  public static final RelType REL_OPENID_XRD =
    new RelType("http://specs.openid.net/auth/2.5/xrd");

  // specifies a link that points to an XRD(S) document that includes some
  // meta-data.
  public static final RelType REL_DESCRIBED_BY = new RelType("describedby");

  // When looking for an XRD that may have information about the OpenID
  // OP in it, we first look for a link that has the most specific rel-types,
  // and if that link doesn't exist, start looking for less specific rel-types.
  private static final Ordering OP_PREFERENCE_ORDER = new Ordering(
      setOf(REL_DESCRIBED_BY, REL_OPENID_OP_XRD),
      setOf(REL_DESCRIBED_BY, REL_OPENID_XRD),
      setOf(REL_DESCRIBED_BY));

  // When looking for an XRD that may have information about the OpenID
  // RP in it, we first look for a link that has the most specific rel-types,
  // and if that link doesn't exist, start looking for less specific rel-types.
  @SuppressWarnings("unused")
  private static final Ordering RP_PREFERENCE_ORDER = new Ordering(
      setOf(REL_DESCRIBED_BY, REL_OPENID_RP_XRD),
      setOf(REL_DESCRIBED_BY, REL_OPENID_XRD),
      setOf(REL_DESCRIBED_BY));

  private final HostMetaFetcher hostMetaFetcher;
  private final XrdDiscoveryResolver<UrlIdentifier> userXrdResolver;
  private final XrdDiscoveryResolver<IdpIdentifier> siteXrdResolver;
  private final HostMetaDiscovery<UrlIdentifier> userHostMetaDiscovery;
  private final HostMetaDiscovery<IdpIdentifier> siteHostMetaDiscovery;

  // Strategy for site discovery: First, we try discoverOpEndpointsForSite,
  // and as a fallback try legacy discovery for an identifier derived from
  // the site identifier
  private final FallbackDiscovery<IdpIdentifier> siteFallbackDiscoverer =
    new FallbackDiscovery<IdpIdentifier>() {
      @Override
      public List<DiscoveryInformation> newStyleDiscovery(IdpIdentifier idp)
          throws DiscoveryException {
        // how new-style discovery is performed
        return discoverOpEndpointsForSite(idp);
      }

      @Override
      public Identifier getLegacyIdentifier(IdpIdentifier idp)
          throws DiscoveryException {
        // which identifier is to be used for the fallback old-style discovery
        return new UrlIdentifier(idp.getIdentifier());
      }
    };

  // Strategy for claimed_id discovery: first, we'll try
  // discoverOpEndpointsForUser, and if that fails simply perform legacy
  // discovery on the same UrlIdentifier
  private final FallbackDiscovery<UrlIdentifier> userFallbackDiscoverer =
    new FallbackDiscovery<UrlIdentifier>() {
      @Override
      public List<DiscoveryInformation> newStyleDiscovery(UrlIdentifier url)
          throws DiscoveryException {
        // how new-style discovery is performed
        return discoverOpEndpointsForUser(url);
      }

      @Override
      public Identifier getLegacyIdentifier(UrlIdentifier url) {
        // which identifier is to be used for the fallback old-style discovery
        return url;
      }
    };

  @Inject
  public Discovery2(HostMetaFetcher hostMetaFetcher,
      XrdDiscoveryResolver<UrlIdentifier> userXrdResolver,
      XrdDiscoveryResolver<IdpIdentifier> siteXrdResolver) {
    this.hostMetaFetcher = hostMetaFetcher;
    this.userXrdResolver = userXrdResolver;
    this.siteXrdResolver = siteXrdResolver;
    this.userHostMetaDiscovery = new HostMetaDiscovery.UserHostMetaDiscovery();
    this.siteHostMetaDiscovery = new HostMetaDiscovery.SiteHostMetaDiscovery();
  }

  /**
   * Returns list of likely OpenID endpoints for a site, ordered by
   * preference as listed by the site. If there is a link in the host-meta
   * that points to the OP, then that link becomes the only DisoveryInformation
   * returned. Otherwise, the host-meta can point to an XRD(S) document, which
   * may contain a (prioritized) list of endpoints, which will be returned.
   *
   * @param site the {@link IdpIdentifier} identifying the site for which
   *   OpenID endpoints are being sought.
   */
  public List<DiscoveryInformation> discoverOpEndpointsForSite(
      IdpIdentifier site) throws DiscoveryException {

    return performHostMetaBasedDiscovery(siteHostMetaDiscovery,
        siteXrdResolver, site.getIdentifier(), site);
  }

  /**
   * Returns list of OpenID endpoints declared by a claimed id (aka user id).
   * There are a variety of ways to discover OP endpoints from a claimed id.
   * We're doing it in the following order: first, try Link-header based
   * discovery. Next, try link-element (in HTML)-based discovery. Last, try
   * host-meta-based discovery. In the latter case, we expect the host-meta
   * to either point directly to the OP endpoint (using Link-Pattern), or
   * to point to a site-wide XRDS, which will in turn contain a URI template
   * allowing us to build URIs for per-user XRDS's.
   *
   * @param claimedId the {@link UrlIdentifier} identifying the user's claimed
   *   id
   */
  public List<DiscoveryInformation> discoverOpEndpointsForUser(
      UrlIdentifier claimedId) throws DiscoveryException {

    List<DiscoveryInformation> result;

    try {
      result = tryLinkHeaderBasedDiscoveryForUser(claimedId);
    } catch (DiscoveryException e) {
      result = null;
    }

    if (result != null) {
      return result;
    }

    try {
      result = tryLinkElementBasedDiscoveryForUser(claimedId);
    } catch (DiscoveryException e) {
      result = null;
    }

    if (result != null) {
      return result;
    }

    return tryHostMetaBasedClaimedIdDiscovery(claimedId);
  }

  /**
   * Returns list of likely OpenID endpoints for a user, ordered by
   * preference. If there is a link-pattern in the host-meta that points to the
   * OP, then that link becomes the only DisoveryInformation
   * returned. Otherwise, the host-meta can point to a site's XRD(S) document,
   * which may contain URITemplates, which in turn point to the user's XRD(S).
   * The latter should include a list of OpenID endpoints.
   *
   * @param claimedId the {@link IdpIdentifier} identifying the site for which
   *   OpenID endpoints are being sought.
   */
  /* visible for testing */
  List<DiscoveryInformation> tryHostMetaBasedClaimedIdDiscovery(
      UrlIdentifier claimedId) throws DiscoveryException {

    // extract the host from the user's URL
    String host = claimedId.getUrl().getHost();

    return performHostMetaBasedDiscovery(userHostMetaDiscovery, userXrdResolver,
        host, claimedId);
  }

  /**
   * Performs host-meta-based OpenID discovery for different form of
   * identifiers (e.g., IdPIdentifier for a site or UrlIdentifier for a user).
   *
   * We first fetch the host-meta for the site, and check whether it
   * contains discovery information appropriate for the type of identifier
   * in question. If not, we look for a pointer to the site-wide XRD(S) in
   * the host-meta, and ask the XrdResolver to look for the metadata in that
   * XRD(S).
   *
   * @param <T> the type of identifier we're performing discovery on
   * @param discovery the object that knows how to look for
   *   identifier-type-specific metadata both in the host-meta and in the XRD(S)
   * @param host the host for which the host-meta should be fetched.
   * @param id the identifier for which we're trying to discover the OpenID
   *   endpoint
   * @throws DiscoveryException
   */
  /* visible for testing */
  <T extends Identifier>
  List<DiscoveryInformation> performHostMetaBasedDiscovery(
      HostMetaDiscovery<T> discovery, XrdDiscoveryResolver<T> xrdResolver,
      String host, T id) throws DiscoveryException {

    // get host-meta for that host
    HostMeta hostMeta;
    try {
      hostMeta = hostMetaFetcher.getHostMeta(host);
    } catch (HostMetaException e) {
      throw new DiscoveryException("could not get host-meta for " + host, e);
    }

    // does host-meta point directly to OP endpoint?
    DiscoveryInformation info =
        discovery.findOpEndpointInHostMeta(hostMeta, id);
    if (info != null) {
      return Collections.singletonList(info);
    }

    // find side-wide XRD that host-meta is pointing to
    URI siteXrdsUri = findSiteXrdUriForOp(hostMeta,
        xrdResolver.getDiscoveryDocumentType());
    if (siteXrdsUri == null) {
      return Collections.emptyList();
    }

    // look for URITemplates in site xrd(s), follow them, and return results
    return xrdResolver.findOpEndpoints(id, siteXrdsUri);
  }

  /**
   * Link-element based discovery.
   */
  private List<DiscoveryInformation> tryLinkElementBasedDiscoveryForUser(
      UrlIdentifier claimedId) throws DiscoveryException {
    // TODO: implement this
    throw new DiscoveryException("link-element-based discovery is not " +
        "implemented yet");
  }

  /**
   * Link-header based discovery.
   */
  private List<DiscoveryInformation> tryLinkHeaderBasedDiscoveryForUser(
      UrlIdentifier claimedId) throws DiscoveryException {
    // TODO: implement this
    throw new DiscoveryException("link-header-based discovery is not " +
        "implemented yet");
  }

  /**
   * Legacy generic discovery method. Checks the type of identifier provided,
   * and dispatches to the appropriate discovery method. Also employs a
   * fallback strategy to use 2.0-style discovery in case the new-style
   * discovery doesn't yield any results.
   */
  @Override
  public List<DiscoveryInformation> discover(Identifier identifier)
      throws DiscoveryException {

    /*
     * The old API doesn't distinguish between discovery of an IdP endpoint
     * and discovery of a user id. We introduce a new type of Identifier
     * to be able to distinguish between these two cases.
     */

    if (identifier instanceof IdpIdentifier) {

      IdpIdentifier site = (IdpIdentifier) identifier;
      return siteFallbackDiscoverer.get(site);

    } else if (identifier instanceof UrlIdentifier) {

      UrlIdentifier url = (UrlIdentifier)identifier;
      return userFallbackDiscoverer.get(url);

    } else {

      // for all other types of identifiers, use old-style discovery
      @SuppressWarnings("unchecked")
      List<DiscoveryInformation> result = super.discover(identifier);
      return result;
    }
  }

  /**
   * Finds, in /host-meta, a pointer to a site-wide XRD(S) document. Normally,
   * this would simply be the URI in Link: entry with rel-type "describedby".
   * But sites can annotate the link with further rel-types, indicating whether
   * the XRD(S) pointed to is likely to contain OpenID-related information or
   * even more specifically, OP or RP-related information.
   *
   * @param hostMeta the host-meta we're searching through.
   * @param mimeType the mime-type of the link we're interested in.
   */
  /* visible for testing */
  URI findSiteXrdUriForOp(HostMeta hostMeta, String mimeType) {

    // bring links into a sortable datatype, and only use those
    // that seem to point to files of the right MIME type
    List<Link> links = filterByMimeType(hostMeta.getLinks(), mimeType);

    if (links.size() < 1) {
      return null;
    }

    // sort according to OpenID discovery preference:
    // since we're looking for an OP for a site, we'll look for something
    // labeled REL_OPENID_OP_XRD (and describedby) first, then for something
    // labeled REL_OPENID_XRD (and describedby), then for something simply
    // labeled "describedby".
    Collections.sort(links, OP_PREFERENCE_ORDER);

    // make sure that the first link in fact points to something that we think
    // might have OpenID data in it.
    Link candidate = links.get(0);
    RelTypes candidateRelTypes = candidate.getRelationships();

    for (RelTypes validRelTypes : OP_PREFERENCE_ORDER.getAllRelTypeSets()) {
      if (candidateRelTypes.containsAll(validRelTypes)) {
        // yes, the first Link in the list lists a combination of RelTypes
        // that's acceptable
        return candidate.getUri();
      }
    }

    // the first Link in the (sorted) list doesn't contain a combination of
    // acceptable RelTypes
    return null;
  }

  /**
   * Discards all links from a host-meta that aren't the right MIME type.
   */
  private List<Link> filterByMimeType(Collection<Link> links, String mimeType) {
    ArrayList<Link> result = new ArrayList<Link>();

    for (Link link : links) {
      if (mimeType.equals(link.getMimeType())) {
        result.add(link);
      }
    }

    return result;
  }

  /**
   * Helper class that implements an induced order, i.e., can order lists
   * according to the priority of RelType sets. For example, if the
   * list of rel-types passed to the constructor is:
   *
   * (foo bar), (foo), (bla)
   *
   * then this class can order a list of links with associated rel-types such
   * that links that have both "foo" and "bar" rel-types come first, then
   * (other) links with rel-type "foo", then links with rel-type "bla", and
   * then all other links.
   */
  static class Ordering implements Comparator<Link> {

    private final List<RelTypes> rels;
    private final Integer maxValue;

    public Ordering(RelTypes... rels) {
      this.rels = Arrays.asList(rels);
      this.maxValue = rels.length;
    }

    public int compare(Link o1, Link o2) {
      return getOrdinal(o1).compareTo(getOrdinal(o2));
    }

    public List<RelTypes> getAllRelTypeSets() {
      return rels;
    }

    private Integer getOrdinal(Link o2) {
      for(int i = 0; i < rels.size(); i++) {
        if (o2.getRelationships().containsAll(rels.get(i))) {
          return i;
        }
      }
      return maxValue;
    }
  }

  /**
   * Implements fallback discovery: First, we try new-style discovery (to
   * be implemented by a subclass), and if that doesn't work, we'll give the
   * implementing subclass a chance to provide a different identifier for the
   * legacy discovery. For example, a subclass implementing discovery for an
   * IdPIdentifier identifying a site (which doesn't exist in 2.0-style
   * discovery) could convert the IdPIdentifier "example.com" into an
   * UrlIdentifier "http://www.example.com/" and have legacy discovery performed
   * on that UrlIdentifier.
   *
   * @param <T> the type of identifier discovery is performed on.
   */
  /* visible for testing */
  abstract class FallbackDiscovery<T extends Identifier> {

    public abstract List<DiscoveryInformation> newStyleDiscovery(T id)
        throws DiscoveryException;

    public abstract Identifier getLegacyIdentifier(T id)
        throws DiscoveryException;

    public List<DiscoveryInformation> get(T id)
        throws DiscoveryException {

      List<DiscoveryInformation> result;

      // first, try new-style discovery
      try {
        result = newStyleDiscovery(id);
        if (result != null && result.size() == 0) {
          result = null;
        }
      } catch (DiscoveryException e) {
        result = null;
      }

      if (result != null) {
        return result;
      }

      // if that doesn't work, try old-style discovery
      return oldStyleDiscovery(getLegacyIdentifier(id));
    }

    @SuppressWarnings("unchecked")
    /* visible for testing */
    List<DiscoveryInformation> oldStyleDiscovery(Identifier id)
        throws DiscoveryException {
      return Discovery2.super.discover(id);
    }
  }
}
