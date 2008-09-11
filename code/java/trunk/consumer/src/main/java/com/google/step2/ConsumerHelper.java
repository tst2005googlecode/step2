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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;

/**
 * Class that lets you generate AuthRequestHelpers. This class is basically
 * a wrapper around a ConsumerManager (which can handle associations,
 * discovery, and standard claimed_id-carrying auth requests).
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class ConsumerHelper {
  private static Log log = LogFactory.getLog(ConsumerHelper.class);

  // for doing associations, for generating requests that include
  // OpenID identity requests, and for verifying responses that include
  // OpenID identity requests.
  private ConsumerManager consumerManager;

  @Inject
  public ConsumerHelper(ConsumerManager consumerManager) {
    this.consumerManager = consumerManager;
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
  public AuthRequestHelper getAuthRequestHelper(String openId,
      String returnToUrl) {
    log.info("OpenId: " + openId + " Return URL: " + returnToUrl);
    return new AuthRequestHelper(consumerManager, openId,
        returnToUrl);
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
      ParameterList authResponse, DiscoveryInformation discovered)
      throws MessageException, AssociationException, DiscoveryException,
      VerificationException {
    log.info("Receiving URL: " + receivingUrl);
    VerificationResult verification =
      consumerManager.verify(receivingUrl, authResponse, discovered);

    // the only indication that something went wrong during nonce and
    // signature checking is if the returned identity is null. In that case,
    // we don't even want to return anything.
    if (verification.getVerifiedId() == null) {
      throw new VerificationException("something went wrong during " +
          "response verification, such as nonce or signature checking. " +
      "Check your debug logs.");
    }

    return new AuthResponseHelper(verification);
  }
}