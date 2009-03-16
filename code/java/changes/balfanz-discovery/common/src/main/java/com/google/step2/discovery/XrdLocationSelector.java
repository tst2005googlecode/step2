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

import org.openid4java.discovery.UrlIdentifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Various strategies for finding the URI pointing to a relevant XRD(S) document.
 *
 * The goal of XRD location selection is to find a suitable URI from a
 * host-meta document that is likely to point to an XRD(S) file with
 * metadata about the (user or IdP) identifier in question.
 *
 */
public class XrdLocationSelector {

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

  /**
   * Returns a URI that points directly to the claimed id's XRD(S) document.
   * The user's XRD(S) document should contain the pointer to the OP.
   */
  public URI findUserXrdUriForOp(HostMeta hostMeta, String mimeType,
      UrlIdentifier claimedId) {

    LinkPattern pattern = getMatchingLink(hostMeta.getLinkPatterns(), mimeType);
    if (pattern == null) {
      return null;
    }

    UriTemplate template = new UriTemplate(pattern.getUriPattern());
    return template.map(URI.create(claimedId.getIdentifier()));
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
  public URI findSiteXrdUriForOp(HostMeta hostMeta, String mimeType) {
    Link link = getMatchingLink(hostMeta.getLinks(), mimeType);
    return (link == null) ? null : link.getUri();
  }

  /**
   * Returns a link or link-pattern (from the collection passed in) that matches
   * the requirements of OP discovery. That is, it needs to have the the
   * specified mime-type, and is preferrably rel-typed as (describedby,
   * http://specs.openid.net/auth/2.5/xrd-op) (although less specific rel-types
   * are also considered if the most specific one cannot be found).
   */
  private <T extends LinkBase> T getMatchingLink(Collection<T> links,
      String mimeType) {

    // bring links into a sortable datatype, and only use those
    // that seem to point to files of the right MIME type
    List<T> sortableLinks = filterByMimeType(links, mimeType);

    if (sortableLinks.size() < 1) {
      return null;
    }

    // sort according to OpenID discovery preference:
    // since we're looking for an OP for a site, we'll look for something
    // labeled REL_OPENID_OP_XRD (and describedby) first, then for something
    // labeled REL_OPENID_XRD (and describedby), then for something simply
    // labeled "describedby".
    Collections.sort(sortableLinks, OP_PREFERENCE_ORDER);

    // make sure that the first link in fact points to something that we think
    // might have OpenID data in it.
    T candidate = sortableLinks.get(0);
    RelTypes candidateRelTypes = candidate.getRelationships();

    for (RelTypes validRelTypes : OP_PREFERENCE_ORDER.getAllRelTypeSets()) {
      if (candidateRelTypes.containsAll(validRelTypes)) {
        // yes, the first Link in the list lists a combination of RelTypes
        // that's acceptable
        return candidate;
      }
    }

    // the first Link in the (sorted) list doesn't contain a combination of
    // acceptable RelTypes
    return null;
  }

  /**
   * Discards all links from a host-meta that aren't the right MIME type.
   */
  private <T extends LinkBase> List<T> filterByMimeType(Collection<T> links,
      String mimeType) {

    ArrayList<T> result = new ArrayList<T>();

    for (T link : links) {
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
  private static class Ordering implements Comparator<LinkBase> {

    private final List<RelTypes> rels;
    private final Integer maxValue;

    public Ordering(RelTypes... rels) {
      this.rels = Arrays.asList(rels);
      this.maxValue = rels.length;
    }

    public int compare(LinkBase o1, LinkBase o2) {
      return getOrdinal(o1).compareTo(getOrdinal(o2));
    }

    public List<RelTypes> getAllRelTypeSets() {
      return rels;
    }

    private Integer getOrdinal(LinkBase o2) {
      for(int i = 0; i < rels.size(); i++) {
        if (o2.getRelationships().containsAll(rels.get(i))) {
          return i;
        }
      }
      return maxValue;
    }
  }
}
