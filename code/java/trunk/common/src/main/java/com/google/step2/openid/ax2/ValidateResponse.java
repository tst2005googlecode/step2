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

package com.google.step2.openid.ax2;

import org.apache.log4j.Logger;
import org.openid4java.message.MessageException;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.StoreResponse;

/**
 * Implementation of a proposed "validate" attribute exchange response 
 * 
 * @author Steve Weis (steveweis@gmail.com)
 */
public class ValidateResponse extends StoreResponse {
  private static final Logger LOG = Logger.getLogger(ValidateResponse.class);
  private static final boolean DEBUG = LOG.isDebugEnabled();
  private boolean success = false;
  
  @Override
  public String getTypeUri() {
    return AxMessage2.OPENID_NS_AX_FINAL;
  }
  
  public ValidateResponse(boolean success) {
    super();
    _parameters.set(new Parameter(AxMessage2.MODE, success ?
        Mode.VALIDATE_RESPONSE_SUCCESS.toString() :
        Mode.VALIDATE_RESPONSE_FAILURE.toString()));
    this.success = success;
    if (success) {
      LOG.debug("Created empty validate success response");
    } else {
      LOG.debug("Created empty validate failure response");
    }
  }

  public ValidateResponse(boolean success, ParameterList params) {
    super(params);
    this.success = success;
    _parameters.set(new Parameter(AxMessage2.MODE, success ?
        Mode.VALIDATE_RESPONSE_SUCCESS.toString() :
        Mode.VALIDATE_RESPONSE_FAILURE.toString()));
    if (success) {
      LOG.debug("Created validate success response");
    } else {
      LOG.debug("Created validate failure response");
    }
  }

  public static ValidateResponse createValidateResponse(boolean success,
      ParameterList params) {
    return new ValidateResponse(success, params);
  }

  public boolean isSuccessful() {
    return success;
  }
}
