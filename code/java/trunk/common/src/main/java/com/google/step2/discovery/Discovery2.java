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

import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 *   points to an XRD(S) for the claimed id, and if so, skip the next two steps.
 * - if not, find a Link in the host-meta that points to an XRD(S) metadata
 *   document for the site.
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

  private static final Logger logger =
      Logger.getLogger(Discovery2.class.getName());

  private final HostMetaFetcher hostMetaFetcher;
  private final XrdDiscoveryResolver xrdResolver;
  private final XrdLocationSelector xrdLocationSelector;

  // Strategy for site discovery: First, we try discoverOpEndpointsForSite,
  // and as a fallback try legacy discovery for an identifier derived from
  // the site identifier
  private final FallbackDiscovery<IdpIdentifier> siteFallbackDiscoverer =
    new FallbackDiscovery<IdpIdentifier>() {
      @Override
      public List<SecureDiscoveryInformation> newStyleDiscovery(
          IdpIdentifier idp) throws DiscoveryException {
        // how new-style discovery is performed
        return discoverOpEndpointsForSite(idp);
      }

      @Override
      public Identifier getLegacyIdentifier(IdpIdentifier idp)
          throws DiscoveryException {
        // which identifier is to be used for the fallback old-style discovery
        try {
          String openIdAsString = idp.getIdentifier().trim();

          // since we're going to convert this to a URL anyway, we might as
          // well make sure that the identifier starts with http[s]://
          // Otherwise, the URL classes used inside UrlIdentifier tend to
          // get huffy.
          URI openIdAsUri = URI.create(openIdAsString);
          if (openIdAsUri.getScheme() == null) {
            openIdAsString = "http://" + openIdAsString;
          }

          return new UrlIdentifier(openIdAsString);
        } catch (IllegalArgumentException e) {
          // thrown if the identifier doesn't look like a host name
          throw new DiscoveryException(e);
        }
      }
    };

  // Strategy for claimed_id discovery: first, we'll try
  // discoverOpEndpointsForUser, and if that fails simply perform legacy
  // discovery on the same UrlIdentifier
  private final FallbackDiscovery<UrlIdentifier> userFallbackDiscoverer =
    new FallbackDiscovery<UrlIdentifier>() {
      @Override
      public List<SecureDiscoveryInformation> newStyleDiscovery(
          UrlIdentifier url) throws DiscoveryException {
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
   * preference as listed by the site. The host-meta points to an XRD(S)
   * document, which contains a (prioritized) list of endpoints, and which
   * will be returned.
   *
   * @param site the {@link IdpIdentifier} identifying the site for which
   *   OpenID endpoints are being sought.
   */
  public List<SecureDiscoveryInformation> discoverOpEndpointsForSite(
      IdpIdentifier site) throws DiscoveryException {

    String host = site.getIdentifier();

    // get host-meta for that host
    HostMeta hostMeta;
    try {
      hostMeta = hostMetaFetcher.getHostMeta(host);
    } catch (HostMetaException e) {
      throw new DiscoveryException("could not get host-meta for " + host, e);
    }

    // Find XRD that host-meta is pointing to. In the case of site-discovery,
    // this will point to the site's XRD.
    URI xrdUri = xrdLocationSelector.findSiteXrdUriForOp(hostMeta,
        xrdResolver.getDiscoveryDocumentType());

    if (xrdUri == null) {
      return Collections.emptyList();
    }

    // now that we have the location of the XRD, perform the actual
    // discovery based on the XRD.
    return xrdResolver.findOpEndpointsForSite(site, xrdUri);
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
  public List<SecureDiscoveryInformation> discoverOpEndpointsForUser(
      UrlIdentifier claimedId) throws DiscoveryException {

    List<SecureDiscoveryInformation> result;

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
   * user's XRD(S), we base XRD(S) discovery on that document.
   * Otherwise, the host-meta can point to a site's XRD(S) document,
   * which may contain URITemplates, which in turn point to the user's XRD(S).
   * The latter should include a list of OpenID endpoints.
   *
   * @param claimedId the {@link IdpIdentifier} identifying the site for which
   *   OpenID endpoints are being sought.
   */
  /* visible for testing */
  List<SecureDiscoveryInformation> tryHostMetaBasedDiscoveryForUser(
      UrlIdentifier claimedId) throws DiscoveryException {

    // extract the host from the claimed id
    String host = claimedId.getUrl().getHost();

    // get host-meta for that host
    HostMeta hostMeta;
    try {
      hostMeta = hostMetaFetcher.getHostMeta(host);
    } catch (HostMetaException e) {
      throw new DiscoveryException("could not get host-meta for " + host, e);
    }

    // First, let's check whether there are link-patterns in the host-meta that
    // point directly to the user's XRD(S).
    URI xrdUri = xrdLocationSelector.findUserXrdUriForOp(hostMeta,
        xrdResolver.getDiscoveryDocumentType(), claimedId);

    if (xrdUri != null) {

      // xrdUri points to user's XRD
      return xrdResolver.findOpEndpointsForUser(claimedId, xrdUri);
    }

    // There were no link-patterns, i.e.,  we'll have to go with the
    // site-wide XRD(S)
    xrdUri = xrdLocationSelector.findSiteXrdUriForOp(hostMeta,
        xrdResolver.getDiscoveryDocumentType());

    if (xrdUri != null) {

      // xrdUri points to site-wide XRD
      return xrdResolver.findOpEndpointsForUserThroughSiteXrd(claimedId,
          xrdUri);
    }

    // xrdUri == null
    return Collections.emptyList();
  }

  /**
   * Link-element based discovery.
   */
  private List<SecureDiscoveryInformation> tryLinkElementBasedDiscoveryForUser(
      UrlIdentifier claimedId) throws DiscoveryException {
    // TODO: implement this
    throw new DiscoveryException("link-element-based discovery is not " +
        "implemented yet");
  }

  /**
   * Link-header based discovery.
   */
  private List<SecureDiscoveryInformation> tryLinkHeaderBasedDiscoveryForUser(
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
  public List<SecureDiscoveryInformation> discover(Identifier identifier)
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
      return convertToNewDiscoveryInfo(result);
    }
  }

  /**
   * Converts {@link DiscoveryInformation} objects into
   * {@link SecureDiscoveryInformation} object (which will have the isSecure
   * bit set to false).
   */
  /* visible for testing */
  static List<SecureDiscoveryInformation> convertToNewDiscoveryInfo(
      List<DiscoveryInformation> infos) throws DiscoveryException {

    if (infos == null) {
      return null;
    }

    List<SecureDiscoveryInformation> result =
        Lists.newArrayListWithCapacity(infos.size());

    for (DiscoveryInformation info : infos) {
      result.add(new SecureDiscoveryInformation(info));
    }
    return result;
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

    public abstract List<SecureDiscoveryInformation> newStyleDiscovery(T id)
        throws DiscoveryException;

    public abstract Identifier getLegacyIdentifier(T id)
        throws DiscoveryException;

    public List<SecureDiscoveryInformation> get(T id)
        throws DiscoveryException {

      List<SecureDiscoveryInformation> result;

      // first, try new-style discovery
      try {
        result = newStyleDiscovery(id);
        if (result != null && result.size() == 0) {
          logger.log(Level.WARNING, "could not perform new-style discovery on "
              + id.getIdentifier() + ". discovery returned null");
          result = null;
        }
      } catch (DiscoveryException e) {
        logger.log(Level.WARNING, "could not perform new-style discovery on "
            + id.getIdentifier(), e);
        result = null;
      }

      if (result != null) {
        return result;
      }

      // if that doesn't work, try old-style discovery
      return convertToNewDiscoveryInfo(
          oldStyleDiscovery(getLegacyIdentifier(id)));
    }

    @SuppressWarnings("unchecked")
    /* visible for testing */
    List<DiscoveryInformation> oldStyleDiscovery(Identifier id)
        throws DiscoveryException {
      return Discovery2.super.discover(id);
    }
  }
}
