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

import com.google.inject.ImplementedBy;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.util.List;

/**
 * Interface describing an XRD discovery resolver. Since we anticipate that the
 * format of OpenID-meta data might change in the future, we provide this
 * interface here that can be implemented by different classes. For example,
 * one implementation might use old-style XRDS syntax to look for OpenID
 * meta-data, while another implementation might use new XRD syntax to do the
 * same job.
 */
@ImplementedBy(LegacyXrdsResolver.class)  // just for now.
public interface XrdDiscoveryResolver {

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
  public String getDiscoveryDocumentType();

  /**
   * Finds OP endpoints for a site in XRD(S) documents.
   * @param site the site identifier on which we're performing discovery
   * @param xrdUri the URL of the site's XRD(S) document that has OpenID
   *   metadata in it.
   * @return a list of discovery info objects. A discovery info object will
   *   include the URL of the discovered endpoint.
   * @throws DiscoveryException
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForSite(
      IdpIdentifier site, URI xrdUri)
      throws DiscoveryException;

  /**
   * Finds OP endpoints for a user in XRD(S) documents.
   * @param claimedId the user's identifier
   * @param xrdUri the URL of the user's XRD(S) document that has OpenID
   *   metadata in it.
   * @return a list of discovery info objects. A discovery info object will
   *   include the URL of the discovered endpoint, the claimed id, and
   *   possibly the OP-local id of a user.
   * @throws DiscoveryException
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForUser(
      UrlIdentifier claimedId, URI xrdUri)
      throws DiscoveryException;

  /**
   * Finds OP endpoints for a site in XRD(S) documents.
   * @param claimedId the user's identifier on which we're performing discovery
   * @param xrdUri the URL of the site's XRD(S) document that has OpenID
   *   metadata in it. The site in question is the site (host) of the user's
   *   claimed id.
   * @return a list of discovery info objects. A discovery info object will
   *   include the URL of the discovered endpoint, the claimed id, and
   *   possibly the OP-local id of a user.
   * @throws DiscoveryException
   */
  public List<SecureDiscoveryInformation> findOpEndpointsForUserThroughSiteXrd(
      UrlIdentifier claimedId, URI xrdUri)
      throws DiscoveryException;
}
