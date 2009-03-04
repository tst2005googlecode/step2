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

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;

/**
 * Abstract super-class for, and implementations of, host-meta-based
 * discovery strategies for various types of Identifiers. Generally, we're
 * looking for links of certain rel-types (depending on the type of identifier)
 * in a host-meta file.
 *
 * @param <T> the type of identifier that we're performing discovery on.
 */
public abstract class HostMetaDiscovery<T extends Identifier> {

  /**
   * Find an OP endpoint in the host-meta document. For different types of
   * identifiers, we have differently-labeled endpoints. For example, for
   * user identifiers, we would be looking for link-patterns of rel-type
   * "http://specs.openid.net/auth/2.0/signon", while for site identifiers,
   * we would be looking for links of rel-type
   * "http://specs.openid.net/auth/2.0/server"
   *
   * @param hostMeta the host-meta in which we're looking for the discovery
   *   information.
   * @param id the id on which we're performing discovery. In some cases, the
   *   id will be included in the returned discovery information. For example,
   *   in the case of a user's UrlIdentifier, the id will become the claimedId
   *   in the returned discovery information.
   */
  public abstract DiscoveryInformation findOpEndpointInHostMeta(
      HostMeta hostMeta, T id) throws DiscoveryException;

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Implementation class for UrlIdentifiers (i.e., user discovery).
   */
  public static class UserHostMetaDiscovery
      extends HostMetaDiscovery<UrlIdentifier> {

    private static final String LOCAL_ID_LINK_PARAM = "local-id";

    @Override
    public DiscoveryInformation findOpEndpointInHostMeta(HostMeta hostMeta,
        UrlIdentifier id) throws DiscoveryException {
      return findOpEndpointForUserInHostMeta(hostMeta, id);
    }

    /* visible for testing */
    DiscoveryInformation findOpEndpointForUserInHostMeta(HostMeta hostMeta,
        UrlIdentifier claimedId) throws DiscoveryException {

      LinkPattern pattern = findUserSignonLinkPatternInHostMeta(hostMeta);

      if (pattern == null) {
        return null;
      }

      return createDiscoveryInfoFromSignonLinkPattern(pattern, claimedId);
    }

    private LinkPattern findUserSignonLinkPatternInHostMeta(HostMeta hostMeta) {

      Collection<LinkPattern> linkPatterns = hostMeta.getLinkPatterns();

      for (LinkPattern pattern : linkPatterns) {
        if (pattern.getRelationships().contains(
            Discovery2.REL_OP_ENDPOINT_SIGNON)) {
          return pattern;
        }
      }

      return null;
    }

    /* visible for testing */
    DiscoveryInformation createDiscoveryInfoFromSignonLinkPattern(
        LinkPattern link, UrlIdentifier claimedId) throws DiscoveryException {

      // apply claimedId to URI template
      URI endpoint = new UriTemplate(link.getUriPattern())
          .map(URI.create(claimedId.getIdentifier()));

      // see whether there was a local-id on the link
      String localId = link.getParamater(LOCAL_ID_LINK_PARAM);

      try {
        return new DiscoveryInformation(endpoint.toURL(), claimedId, localId,
            DiscoveryInformation.OPENID2);
      } catch (MalformedURLException e) {
        throw new DiscoveryException("could not convert mapped URI to URL: " +
          endpoint.toASCIIString(), e);
      } catch (IllegalArgumentException e) {
        throw new DiscoveryException("found OP endpoint link in host-meta, " +
            "but mapped URI was not absolute: " + endpoint.toString(), e);
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Implementation class for IdPIdentifiers (i.e., site discovery).
   */
  public static class SiteHostMetaDiscovery
      extends HostMetaDiscovery<IdpIdentifier> {

    @Override
    public DiscoveryInformation findOpEndpointInHostMeta(HostMeta hostMeta,
        IdpIdentifier id) throws DiscoveryException {
      return findOpEndpointForSiteInHostMeta(hostMeta);
    }

    /* visible for testing */
    DiscoveryInformation findOpEndpointForSiteInHostMeta(HostMeta hostMeta)
        throws DiscoveryException {

      Link link = findServerLinkInHostMeta(hostMeta);
      if (link == null) {
        return null;
      }

      try {
        return new DiscoveryInformation(link.getUri().toURL());
      } catch (MalformedURLException e) {
        throw new DiscoveryException("found OP endpoint link in host-meta, " +
            "but URL was malformed: " + link.getUri().toString(), e);
      } catch (IllegalArgumentException e) {
        throw new DiscoveryException("found OP endpoint link in host-meta, " +
            "but URL was not absolute: " + link.getUri().toString(), e);
      }
    }

    /**
     * Looks for a link in host-meta that points directly to an endpoint of
     * type http://specs.openid.net/auth/2.0/server.
     */
    private Link findServerLinkInHostMeta(HostMeta hostMeta) {
      Collection<Link> links = hostMeta.getLinks();

      for (Link link : links) {
        if (link.getRelationships().contains(Discovery2.REL_OP_ENDPOINT_SERVER)) {
          return link;
        }
      }
      return null;
    }
  }
}
