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

import java.net.URI;
import java.util.List;

/**
 * All XrdDiscoveryResolvers will have to distinguish between user discovery
 * and site discovery. So we're giving them a super class here that teases out
 * the different kinds of identifiers and calls into to-be-implemted methods
 * for user- and site-discovery.
 */
public abstract class AbstractXrdDiscoveryResolver
    implements XrdDiscoveryResolver {

  public List<DiscoveryInformation> findOpEndpoints(Identifier id, URI xrdUri)
      throws DiscoveryException {

    if (id instanceof UrlIdentifier) {
      return findOpEndpointsForUser((UrlIdentifier) id, xrdUri);

    } else if (id instanceof IdpIdentifier) {
      return findOpEndpointsForSite(xrdUri);

    } else {
      throw new DiscoveryException("unkown type of identifier: "
          + id.getClass().getName());
    }
  }

  /**
   * Finds OP endpoints for a user in XRD(S) documents.
   * @param claimedId the identifier on which we're performing discovery
   * @param xrdUri the URL of the XRD(S) document that has OpenID metadata in
   *   it.
   * @return a list of discovery info objects. A discovery info object will
   *   include the claimed id and possibly the OP-local id of the user.
   * @throws DiscoveryException
   */
  protected abstract List<DiscoveryInformation> findOpEndpointsForUser(
      UrlIdentifier claimedId, URI xrdUri) throws DiscoveryException;

  /**
   * Finds OP endpoints for a site in XRD(S) documents.
   * @param xrdUri the URL of the XRD(S) document that has OpenID metadata in
   *   it.
   * @return a list of discovery info objects. A discovery info object will
   *   include simply the URL of the discovered endpoint.
   * @throws DiscoveryException
   */
  protected abstract List<DiscoveryInformation> findOpEndpointsForSite(
      URI xrdUri) throws DiscoveryException;

  /**
   * Returns the mime-type of the document that the implementation knows how
   * to parse. This is given as a hint to the host-meta parser as it looks for
   * Link: entries in the host-meta document that point to documents that can
   * be parsed by an implementing class.
   *
   * The legacy implementation {@link LegacyXrdsResolver} returns
   * application/xrds+xml here, while newer implementations might return
   * application/xrd+xml here.
   */
  public abstract String getDiscoveryDocumentType();
}
