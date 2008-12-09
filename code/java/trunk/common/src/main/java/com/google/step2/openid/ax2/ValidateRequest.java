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
import org.openid4java.message.ax.StoreRequest;

import java.util.Iterator;

/**
 * Implementation of a proposed "validate" attribute exchange request
 *
 * @author Steve Weis (steveweis@gmail.com)
 */
public class ValidateRequest extends StoreRequest {
  private static final Logger LOG = Logger.getLogger(ValidateRequest.class);
  private static final boolean DEBUG = LOG.isDebugEnabled();

  @Override
  public String getTypeUri() {
    return AxMessage2.OPENID_NS_AX_FINAL;
  }

  public ValidateRequest() {
    _parameters.set(
        new Parameter(AxMessage2.MODE, Mode.VALIDATE_REQUEST.toString()));

    if (DEBUG) LOG.debug("Created empty validate request.");
  }

  public ValidateRequest(ParameterList params) {
    super(params);

    _parameters.set(
        new Parameter(AxMessage2.MODE, Mode.VALIDATE_REQUEST.toString()));

    if (DEBUG) LOG.debug("Created validate request.");

  }

  public static ValidateRequest createValidateRequest(ParameterList params)
    throws MessageException {
      ValidateRequest req = new ValidateRequest(params);

      if (!req.isValid()) {
        if (DEBUG) LOG.debug("Invalid parameters for validate request");
        throw new MessageException("Invalid parameters for a validate request");
      }
      if (DEBUG) LOG.debug("Created validate request");
      return req;
  }
}
