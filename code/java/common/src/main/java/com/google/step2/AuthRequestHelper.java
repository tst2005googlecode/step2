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

import com.google.step2.openid.ax2.FetchRequest2;

import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;

import java.util.List;

/**
 * Class that helps build AuthRequest objects. It's usually used like this:
 *
 * <pre>
 * protected void doPost(HttpServletRequest req, HttpServletResponse resp)
 *     throws ServletException, IOException {
 *
 *   ConsumerHelper consumerHelper = ... // recommendation: inject with Guice
 *   String user_supplied_id = ... // id supplied by the user, or perhaps IdP
 *   String returnToUrl = ... // where should IdP send AuthResponse?
 *
 *   AuthRequestHelper helper =
 *       consumerHelper.getAuthRequestHelper(user_supplied_id, returnToUrl);
 *
 *   try {
 *     AuthRequest authReq = helper
 *           .requestAxAttribute("email", AX_EMAIL_SCHEMA, true)
 *           .generateRequest();
 *
 *     resp.sendRedirect(authReq.getDestinationUrl(true));
 *   } catch (DiscoveryException e) {
 *     ...
 *   } catch (MessageException e) {
 *     ...
 *   } catch (ConsumerException e) {
 *     ...
 *   }
 * }
 * </pre>
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class AuthRequestHelper {

  // for doing associations, and for generating requests that include
  // OpenID identity requests
  private final ConsumerManager consumerManager;

  // the user-supplied identifier
  private final String openId;

  // the URL that the auth result should be delivered to
  private final String returnToUrl;

  // if AX attributes are specifies, this object will hold them.
  private FetchRequest2 axFetchRequest = null;

  // discovery information
  private DiscoveryInformation discovered = null;

  AuthRequestHelper(ConsumerManager consumerManager, String openId, String returnToUrl) {
    this.consumerManager = consumerManager;
    this.openId = openId.trim();
    this.returnToUrl = returnToUrl;
  }

  /**
   * Returns discover information obtained by performing discovery on a
   * user-supplied identifier.
   *
   * @throws DiscoveryException if discovery cannot be performed.
   */
  public DiscoveryInformation getDiscoveryInformation()
      throws DiscoveryException {
    // if discovered == null, we'll try again
    if (discovered == null) {
      @SuppressWarnings("unchecked")
      List<DiscoveryInformation> discoveries = consumerManager.discover(openId);
      discovered = consumerManager.associate(discoveries);
    }

    return discovered;
  }

  /**
   * Adds a request for an Attribute Exchange attribute to the outgoing auth
   * request.
   *
   * @param alias which alias should be used for this AX attribute in the
   *   outgoing auth request.
   * @param typeUri describes which attribute we're actually asking about
   *   (see www.axschema.org)
   * @param required whether or not we consider this attribute "required".
   * @return this very instance.
   */
  public AuthRequestHelper requestAxAttribute(String alias, String typeUri,
      boolean required) {
    return requestAxAttribute(alias, typeUri, required, 1);
  }

  /**
   * Adds a request for an Attribute Exchange attribute to the outgoing auth
   * request.
   *
   * @param alias which alias should be used for this AX attribute in the
   *   outgoing auth request.
   * @param typeUri describes which attribute we're actually asking about
   *   (see www.axschema.org)
   * @param required whether or not we consider this attribute "required".
   * @param count how many values for this attribute we would like to receive
   *   in the response.
   * @return this very instance.
   */
  public AuthRequestHelper requestAxAttribute(String alias, String typeUri,
      boolean required, int count) {
    if (axFetchRequest == null) {
      axFetchRequest = new FetchRequest2();
    }

    axFetchRequest.addAttribute(alias, typeUri, required, count);
    return this;
  }

  /**
   * Generates a new auth request, which can be queried for a redirect-URL
   * to send the user agent to (and which will point to he OP).
   */
  public AuthRequest generateRequest() throws DiscoveryException,
      MessageException, ConsumerException {
    DiscoveryInformation discovered = getDiscoveryInformation();

    // this a standard OpenID request
    AuthRequest authReq = consumerManager.authenticate(discovered, returnToUrl);
    if (axFetchRequest != null) {
      authReq.addExtension(axFetchRequest);
    }

    return authReq;
  }
}