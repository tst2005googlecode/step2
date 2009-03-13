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

import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Implements Next-Generation OpenID discovery, based on Link-headers,
 * link-elements, and host-meta. There are two different discovery
 * operations available:
 *
 * (1) discover the OP endpoint(s) for a "site", and
 * (2) discover the OP endpoint(s) for an OpenID (aka claimed id, which is the
 *     id of a user)
 *
 * Case (1) is for when users merely indicate the IdP to the RP, case (2) is
 * for when users actually submit their OpenID (claimed id) to the RP, and also
 * used during the validation of an authentication response from an IdP.
 *
 * The strategy for case (1) is as follows:
 *
 * - find the host-meta file for the site.
 * - find a link in the host-meta that points to an XRD(S) metadata
 *   document for the site
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * The strategy for case (2) is as follows:
 *
 * - Try host-meta strategy (2a)
 * - If that fails, link-header strategy (2b)
 * - If that fails, try link-element strategy (2c)
 *
 * Strategy (2a) works as follows:
 *
 * - find the host-meta file for the host identified in the claimed id.
 * - in the host-meta file, check whether a Link-Pattern in the host-meta
 *   points to an XRD(S) for the OpenID URL.
 * - if not, find a Link in the host-meta that points to an XRD(S) metadata
 *   document for the site
 * - follow the URITemplate link in the site XRD(S) to find the XRD(S) for the
 *   the claimed id.
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * Strategy (2b) works as follow:
 *
 * - Find a link in the HTTP headers in the HTTP response that points
 *   to an XRD(S) metadata document for the claimed id.
 * - follow the links in the XRD(S) to find the OP endpoint.
 *
 * Strategy (2c) works as follow:
 *
 * - Find an HTML link element in the document returned from
 *   the claimed id that points to an XRD(S) metadata document for the
 *   claimed id.
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

  private final HostMetaFetcher hostMetaFetcher;
  private final XrdDiscoveryResolver xrdResolver;
  private final XrdLocationSelector xrdLocationSelector;

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
      XrdDiscoveryResolver xrdResolver) {
    this.hostMetaFetcher = hostMetaFetcher;
    this.xrdResolver = xrdResolver;
    this.xrdLocationSelector = new XrdLocationSelector();
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

    return performHostMetaBasedDiscovery(site.getIdentifier(), site);
  }

  /**
   * Returns list of OpenID endpoints declared by a claimed id (aka user id).
   * There are a variety of ways to discover OP endpoints from a claimed id.
   * We're doing it in the following order: first, try host-meta-based
   * discovery. Next, try link-header-based discovery. Last, try
   * link-element (in HTML)-based discovery.
   *
   * In the latter two cases, the link will point directly to the XRD(S) of the
   * user (claimed id). In the first case, there will either be a Link-Pattern:
   * pointing to the XRD(S) of the user, or a Link: pointing to a site-wide
   * XRD(S) (which in turn will have URITemplates that point to the XRD(S) of
   * the user).
   *
   * @param claimedId the {@link UrlIdentifier} identifying the user's claimed
   *   id
   */
  public List<DiscoveryInformation> discoverOpEndpointsForUser(
      UrlIdentifier claimedId) throws DiscoveryException {

    List<DiscoveryInformation> result;

    try {
      result = tryHostMetaBasedDiscoveryForUser(claimedId);
    } catch (DiscoveryException e) {
      result = null;
    }

    if (result != null) {
      return result;
    }

    try {
      result = tryLinkHeaderBasedDiscoveryForUser(claimedId);
    } catch (DiscoveryException e) {
      result = null;
    }

    if (result != null) {
      return result;
    }

    return tryLinkElementBasedDiscoveryForUser(claimedId);
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
  List<DiscoveryInformation> tryHostMetaBasedDiscoveryForUser(
      UrlIdentifier claimedId) throws DiscoveryException {

    // extract the host from the user's URL
    String host = claimedId.getUrl().getHost();

    return performHostMetaBasedDiscovery(host, claimedId);
  }

  /**
   * Performs host-meta-based OpenID discovery for different form of
   * identifiers (e.g., IdPIdentifier for a site or UrlIdentifier for a user).
   *
   * @param host the host for which the host-meta should be fetched.
   * @param id the identifier for which we're trying to discover the OpenID
   *   endpoint
   * @throws DiscoveryException
   */
  /* visible for testing */
  List<DiscoveryInformation> performHostMetaBasedDiscovery(String host,
      Identifier id) throws DiscoveryException {

    // get host-meta for that host
    HostMeta hostMeta;
    try {
      hostMeta = hostMetaFetcher.getHostMeta(host);
    } catch (HostMetaException e) {
      throw new DiscoveryException("could not get host-meta for " + host, e);
    }

    // Find XRD that host-meta is pointing to. In the case of site-discovery,
    // this will point to the site's XRD. In the case of user-discovery, this
    // can either point directly to the user's XRD, or point to the site's XRD.
    // In this case, the xrdResolver will have to figure out how to get the
    // user's XRD from the site's XRD.
    URI xrdUri = xrdLocationSelector.findXrdUriForOp(hostMeta,
        xrdResolver.getDiscoveryDocumentType(), id);

    if (xrdUri == null) {
      return Collections.emptyList();
    }

    // now that we have the location of the XRD, perform the actual
    // discovery on the XRD. This might involve simply looking for the correct
    // Link in the XRD, or it might involve following URITemplate links, etc.,
    // depending on the kind of Identifier we're performing discovery on.
    return xrdResolver.findOpEndpoints(id, xrdUri);
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
