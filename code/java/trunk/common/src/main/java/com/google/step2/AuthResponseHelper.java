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

import com.google.step2.hybrid.HybridOauthMessage;
import com.google.step2.hybrid.HybridOauthResponse;
import com.google.step2.openid.ax2.AxMessage2;
import com.google.step2.openid.ax2.ValidateResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthFailure;
import org.openid4java.message.AuthImmediateFailure;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchResponse;

import java.util.Collections;
import java.util.List;

/**
 * Class that makes it easier to handle responses from the IdP.
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class AuthResponseHelper {
  private static Log log = LogFactory.getLog(AuthResponseHelper.class);

  public enum ResultType {
    AUTH_SUCCESS,  // response is of type AuthSuccess
    AUTH_FAILURE,  // response is of type AuthFailure
    SETUP_NEEDED,  // response is of type AuthImmediateFailure
    UNKNOWN        // response is of unknown type
  }

  private final VerificationResult result;

  AuthResponseHelper(VerificationResult verification) {
    this.result = verification;
  }

  /**
   * Returns what kind of response we got from the IdP. AUTH_SUCCESS means
   * openid.mode was "id_res", and getAuthResponse() will return an object of
   * type AuthSuccess. SETUP_NEEDED means that openid.mode was "setup_needed"
   * (or some other combination of parameters signaling a failed
   * checkid_immediate), and getAuthResponse() will return an object of type
   * AuthImmediateFailure. AUTH_FAILURE means that openid.mode was "cancel", and
   * getAuthResponse() will return an object of type AuthImmediateFailure.
   *
   * @return the {@link ResultType}.
   */
  public ResultType getAuthResultType() {
    log.info(result.getAuthResponse());
    if (result.getAuthResponse() instanceof AuthSuccess) {
      return ResultType.AUTH_SUCCESS;
    } else if (result.getAuthResponse() instanceof AuthImmediateFailure) {
      return ResultType.SETUP_NEEDED;
    } else if (result.getAuthResponse() instanceof AuthFailure) {
      return ResultType.AUTH_FAILURE;
    } else {
      return ResultType.UNKNOWN;
    }
  }

  /**
   * Returns the response message itself. Use getAuthResultType to figure out
   * which subclass this can be safely cast to.
   *
   * @return The response message.
   */
  public Message getAuthResponse() {
    return result.getAuthResponse();
  }

  /**
   * If response was ResulType.SETUP_NEEDED, then this will return the endpoint
   * to contact for the setup.
   *
   * @return the IdP setup URL.
   */
  public String getIdpSetupUrl() {
    log.info(result.getOPSetupUrl());
    return result.getOPSetupUrl();
  }

  /**
   * @returns The status message of the verification step.
   */
  public String getStatusMsg() {
    log.info(result.getStatusMsg());
    return result.getStatusMsg();
  }

  /**
   * If the response included a claimed_id and verified correctly, then this
   * method will return the claimed id.
   *
   * @return verified id claimed by the user, null if the response is not a
   *   successful AuthResponse or doesn't include a claimed id
   */
  public Identifier getClaimedId() {
    if (getAuthResultType() == ResultType.AUTH_SUCCESS) {
      return result.getVerifiedId(); 
    } else {
      return null;
    }
  }
  
  private <T extends MessageExtension> T getExtension(Class<T> type,
      String typeUri) throws MessageException {
    if (!getAuthResponse().hasExtension(typeUri)) {
      throw new MessageException("Response does not have extension for type: "
          + typeUri);
    }
    
    MessageExtension ext = getAuthResponse().getExtension(typeUri);
    if (type.isAssignableFrom(ext.getClass())) {
      return (T) ext;
    }
    throw new MessageException("Cannot cast type " + ext.getClass().getName() +
        " to " + type.getName());
  }
  
  /**
   * @return True if response message includes the Oauth extension. 
   */
  public boolean hasHybridOauthExtension() {
    return getAuthResponse().hasExtension(HybridOauthMessage.OPENID_NS_OAUTH);
  }
  
  /**
   * Returns a HybridOauthResponse if available
   * 
   * @return a HybridOauthResponse object
   *
   * @throws MessageException If the hybrid message parameters were not included
   * in this message
   */
  public HybridOauthResponse getHybridOauthResponse() throws MessageException {
    return getExtension(HybridOauthResponse.class,
        HybridOauthMessage.OPENID_NS_OAUTH);
  }
  
  public Class<? extends AxMessage> getAxExtensionType() throws MessageException {
    Message resp = getAuthResponse();
    if (resp.hasExtension(AxMessage2.OPENID_NS_AX_FINAL)) {
      MessageExtension extension = 
        resp.getExtension(AxMessage2.OPENID_NS_AX_FINAL);
      if (extension instanceof ValidateResponse) {
        return ValidateResponse.class;
      } else if (extension instanceof FetchResponse) {
        return FetchResponse.class;
      }
    }
    return null;
  }

  /**
   * @return True if response message includes the Attribute Exchange extension. 
   */
  public boolean hasAxExtension() {
    return getAuthResponse().hasExtension(AxMessage2.OPENID_NS_AX_FINAL);
  }

  /**
   * Returns the complete ValidateResponse object representing the result of a
   * ValidateRequest
   *
   * @return a ValidateResponse object
   *
   * @throws MessageException if Attribute Extension parameters were not
   *   included in the response, or if some other error occurred.
   */
  public ValidateResponse getAxValidateResponse() throws MessageException {
    return getExtension(ValidateResponse.class, AxMessage2.OPENID_NS_AX_FINAL);
  }
  
  /**
   * Returns the complete FetchResponse object representing all the attributes
   * returned through the Attribute Extension.
   *
   * @return a FetchResponse object
   *
   * @throws MessageException if Attribute Extension parameters were not
   *   included in the response, or if some other error occurred.
   */
  public FetchResponse getAxFetchResponse() throws MessageException {
    return getExtension(FetchResponse.class, AxMessage2.OPENID_NS_AX_FINAL);
  }
  

  /**
   * Returns a list of AX attribute values.
   *
   * @param alias the alias under which the attribute was requested.
   * @return list of attribute values.
   *
   */
  public List<String> getAxFetchAttributeValues(String alias) {
    FetchResponse resp;
    try {
      resp = getAxFetchResponse();
    } catch (MessageException e) {
      return Collections.EMPTY_LIST;
    }

    // when the parameter is not there, then the library returns a list of
    // length 1 with null as its only member. Go figure.
    List<String> result = resp.getAttributeValues(alias);

    if ((result.size() == 1) && (null == result.get(0))) {
      return Collections.EMPTY_LIST;
    } else {
      return result;
    }
  }

  /**
   * Gets an AX attribute value. If there are more than one value returned,
   * this method returns just one of the returned values. If there are no
   * values returned, this method returns null.
   * @param alias the alias under which the attribute was requested.
   */
  public String getAxFetchAttributeValue(String alias) {
    List<String> values = getAxFetchAttributeValues(alias);

    if (values.isEmpty()) {
      return null;
    }

    return values.get(0);
  }
}
