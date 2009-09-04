/**
 * Copyright 2008 Google Inc.
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

package com.google.step2;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.step2.discovery.Discovery2;
import com.google.step2.discovery.SecureDiscoveryInformation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;

import java.util.List;

/**
 * Class that lets you generate AuthRequestHelpers. This class is basically
 * a wrapper around a ConsumerManager (which can handle associations,
 * discovery, and standard claimed_id-carrying auth requests).
 *
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
@Singleton
public class ConsumerHelper {
  private static Log log = LogFactory.getLog(ConsumerHelper.class);

  // for doing associations, for generating requests that include
  // OpenID identity requests, and for verifying responses that include
  // OpenID identity requests.
  private final ConsumerManager consumerManager;

  @Inject
  public ConsumerHelper(ConsumerManager consumerManager, Discovery2 discovery) {
    this.consumerManager = consumerManager;
    this.consumerManager.setDiscovery(discovery);
  }

  /**
   * Create a new AuthRequestHelper object. This is a lightweight operation
   * and can be done on each incoming request.
   *
   * @param openId the user-supplied identifier. In this OpenID 2-world, this
   *  is often just the name of an IdP against which discover is performed, but
   *  it could also be a URL that a user wants to claim ownership of, or an XRI.
   * @param returnToUrl the URL to which the AuthResponse should be sent.
   *
   * @return an AuthRequestHelper object
   */
  public AuthRequestHelper getAuthRequestHelper(Identifier openId,
      String returnToUrl) {
    log.info("OpenId: " + openId + " Return URL: " + returnToUrl);
    return new AuthRequestHelper(consumerManager, openId, returnToUrl);
  }

  /**
   * Verifies an response from the IdP and creates, if verification succeeds,
   * an AuthResponseHelper object.
   *
   * @param receivingUrl the URL at which this response was received (we check
   *   that this matches the return_to URL mentioned in the response itself)
   * @param authResponse the key-value pairs that make up the response we
   *   received.
   * @param discovered discovery information about the IdP, if available (can
   *   be null)
   * @return an AuthResponseHelper
   *
   * @throws AssociationException when there's trouble looking up association
   *   handles, etc.
   * @throws DiscoveryException if we can't connect to the IdP to verify
   *   identities, etc.
   * @throws VerificationException if signature or nonce don't verify.
   * @throws MessageException for other problems.
   */
  public AuthResponseHelper verify(String receivingUrl,
      ParameterList authResponse, DiscoveryInformation discovered) throws
      MessageException, AssociationException, DiscoveryException,
      VerificationException {
    log.info("Receiving URL: " + receivingUrl);

    // we'll do discovery right now. This will prevent the consumerManager
    // from repeating discovery later, and will allow us to modify the
    // VerificationResult depending on whether the discovery was secure.
    SecureDiscoveryInformation d2 = getDiscoveryInfoForClaimedId(authResponse,
        discovered);

    VerificationResult verification =
      consumerManager.verify(receivingUrl, authResponse, d2);

    // the only indication that something went wrong during nonce and
    // signature checking is if the returned identity is null. In that case,
    // we don't even want to return anything.
    if ((verification.getAuthResponse() instanceof AuthSuccess)
        && (verification.getVerifiedId() == null)) {
      throw new VerificationException("something went wrong during " +
          "response verification, such as nonce or signature checking. " +
          "Check your debug logs.");
    }

    boolean secure = checkResponse(d2, verification);

    return new AuthResponseHelper(verification, secure);
  }


  /**
   * Makes sure that the VerificationResult matches the discovery information.
   * @param d2
   * @param verification
   * @return
   */
  private boolean checkResponse(SecureDiscoveryInformation d2,
      VerificationResult verification) {

    if (d2 == null) {
      return false;
    }

    UrlIdentifier claimedIdWithoutFragment;
    try {
      claimedIdWithoutFragment = new UrlIdentifier(
          verification.getVerifiedId().getIdentifier(), true);
    } catch (DiscoveryException e) {
      return false;
    }

    if (d2.getClaimedIdentifier() == null) {
      return false;
    }

    if (!d2.getClaimedIdentifier().getIdentifier().equals(
        claimedIdWithoutFragment.getIdentifier())) {
      return false;
    }

    return d2.isSecure();
  }

  /**
   * Checks whether the supplied {@link DiscoveryInformation} objects is a
   * suitable discovery info object for this auth response's claimed id (i.e.,
   * it needs to be a new-style {@link SecureDiscoveryInformation} object, and
   * it needs to match the auth response). If not, we perform discovery on the
   * claimed identifier (minus fragment) in the auth response, and return a
   * suitable {@link SecureDiscoveryInformation} object.
   *
   * Some of the code in this method is copied from openid4java.net, and is
   * copyright Sxip Identity Corporation.
   *
   * @param authResponse
   * @param discovered
   * @return could either be the provided {@link DiscoveryInformation} object,
   *   an object representing newly discovered information, or null.
   * @throws DiscoveryException
   * @throws MessageException
   */
  private SecureDiscoveryInformation getDiscoveryInfoForClaimedId(
      ParameterList authResponse, DiscoveryInformation discovered)
      throws DiscoveryException, MessageException {

    // we're only interested if this is a successful auth response.
    if (!"id_res".equals(authResponse.getParameterValue("openid.mode"))) {
      return null;
    }

    AuthSuccess authResp = AuthSuccess.createAuthSuccess(authResponse);

    // also, if the auth response isn't well-formed, we're not bothering with
    // the discovery. The consumer manager will reject this later on.
    if (authResp == null || ! authResp.isVersion2() ||
        authResp.getIdentity() == null || authResp.getClaimed() == null) {
      return null;
    }

    // asserted identifier in the AuthResponse
    String assertId = authResp.getIdentity();

    // claimed identifier in the AuthResponse, without fragment
    Identifier respClaimed = consumerManager.getDiscovery()
        .parseIdentifier(authResp.getClaimed(), true);

    // the OP endpoint sent in the response
    String respEndpoint = authResp.getOpEndpoint();

    // now let's check whether we already have new-style discovery information
    // for this claimed id
    if ((discovered instanceof SecureDiscoveryInformation) // implies non-null
        && discovered.hasClaimedIdentifier()
        && discovered.getClaimedIdentifier().equals(respClaimed)) {

      // OP-endpoint, OP-specific ID and protocol version must match
      String opSpecific = discovered.hasDelegateIdentifier()
          ? discovered.getDelegateIdentifier()
          : discovered.getClaimedIdentifier().getIdentifier();

      if (opSpecific.equals(assertId)
          && discovered.isVersion2()
          && discovered.getOPEndpoint().toString().equals(respEndpoint)) {
            return (SecureDiscoveryInformation) discovered;
      }
    }

    // ok, the discovery information provided was either not new-style,
    // or didn't match the auth response.

    // perform discovery on the claimed identifier in the assertion
    @SuppressWarnings("unchecked")
    List<SecureDiscoveryInformation> discoveries =
        consumerManager.getDiscovery().discover(respClaimed);

    SecureDiscoveryInformation firstServiceMatch = null;

    // find the newly discovered service endpoint that matches the assertion
    // - OP endpoint, OP-specific ID and protocol version must match
    // - prefer (first = highest priority) endpoint with an association
    for (SecureDiscoveryInformation service : discoveries) {

      if (DiscoveryInformation.OPENID2_OP.equals(service.getVersion())) {
        continue;
      }

      String opSpecific = service.hasDelegateIdentifier()
          ? service.getDelegateIdentifier()
          : service.getClaimedIdentifier().getIdentifier();

      if (!opSpecific.equals(assertId)
          || !service.isVersion2()
          || !service.getOPEndpoint().toString().equals(respEndpoint)) {
        continue;
      }

      // keep the first endpoint that matches
      if (firstServiceMatch == null) {
        firstServiceMatch = service;
      }

      // we'll keep looking for a service for which we already have an
      // association. Only if we don't find any do we return the first match
      Association assoc = consumerManager.getPrivateAssociationStore().load(
          service.getOPEndpoint().toString(),
          authResp.getHandle());

      // don't look further if there is an association with this endpoint
      if (assoc != null) {
        return service;
      }
    }

    // could be null
    return firstServiceMatch;
  }
}