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

import com.google.step2.openid.ax2.AxMessage2;

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
   * @return the response message.
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
    log.info(result.getIdpSetupUrl());
    return result.getIdpSetupUrl();
  }

  /**
   * Returns the status message of the verification step.
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
    return (getAuthResultType() == ResultType.AUTH_SUCCESS)
           ? result.getVerifiedId()
           : null;
  }

  /**
   * Returns true if response message includes the Attribute Exchange extension.
   */
  public boolean hasAxExtension() {
    return getAuthResponse().hasExtension(AxMessage2.OPENID_NS_AX_FINAL);
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
  public FetchResponse getAxFetchRespone() throws MessageException {
    MessageExtension ext =
        getAuthResponse().getExtension(AxMessage2.OPENID_NS_AX_FINAL);

    if (ext instanceof FetchResponse) {
      return (FetchResponse)ext;
    } else {
      throw new MessageException("expected response type FetchResponse, " +
      		"but got " + ext.getClass().getName());
    }
  }

  /**
   * Returns a list of AX attribute values.
   *
   * @param alias the alias under which the attribute was requested.
   * @return list of attribute values.
   *
   */
  public List<String> getAxAttributeValues(String alias) {

    FetchResponse resp;
    try {
      resp = getAxFetchRespone();
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
  public String getAxAttributeValue(String alias) {
    List<String> values = getAxAttributeValues(alias);

    if (values.isEmpty()) {
      return null;
    }

    return values.get(0);
  }
}
