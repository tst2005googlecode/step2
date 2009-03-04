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

import com.google.inject.TypeLiteral;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
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
 *
 * Note that we define two Guice type literals here. The {@link Discovery2}
 * class expects two objects, one of each kind. So in your Guice module you will
 * have to specify an implementing class for each of the type literals.
 *
 * If you bind them like this:
 *
 *   bind(XrdDiscoveryResolver.USER_TYPE)
 *       .to(LegacyXrdsResolver.UserXrdsResolver.class).in(Scopes.SINGLETON);
 *
 *   bind(XrdDiscoveryResolver.SITE_TYPE)
 *       .to(LegacyXrdsResolver.SiteXrdsResolver.class).in(Scopes.SINGLETON);
 *
 * Then you will get the "legacy" implementation, i.e., an implementation based
 * on the old-style XRDS syntax.
 *
 * @param <T> the type of identifier that discovery is performed on. We support
 *   at least two types of identifiers: IdpIdentifier for sites, and
 *   UrlIdentifier for users. The {@link Discovery2} class requires
 *   implementations for both of those identifiers.
 */
public interface XrdDiscoveryResolver<T extends Identifier> {

  public static TypeLiteral<XrdDiscoveryResolver<UrlIdentifier>>
    USER_TYPE = new TypeLiteral<XrdDiscoveryResolver<UrlIdentifier>>() {};

  public static TypeLiteral<XrdDiscoveryResolver<IdpIdentifier>>
    SITE_TYPE = new TypeLiteral<XrdDiscoveryResolver<IdpIdentifier>>() {};

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
   * Finds OP endpoints in XRD(S) documents.
   * @param id the identifier on which we're performing discovery
   * @param siteXrdUri the URL of the site-wide XRD(S) document that has
   *   OpenID metadata in it.
   * @return a list of discovery info objects. A discovery info object could
   *   include simply the URL of the discovered endpoint, or it could include
   *   more information, like the claimed id and OP-local id of a user.
   * @throws DiscoveryException
   */
  public List<DiscoveryInformation> findOpEndpoints(T id, URI siteXrdUri)
      throws DiscoveryException;
}
