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

import org.openid4java.association.Association;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Message;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;

/**
 * Temporary fix to enable more flexible request handling.
 *
 * @author balfanz@google.com (Dirk Balfanz)
 */
public class GenericAuthSuccess extends AuthSuccess {

  private GenericAuthSuccess(ParameterList params) {
    super(params);
  }

  public static AuthSuccess createAuthSuccessMessage(String opEndpoint,
      String returnTo, String nonce, String invalidateHandle,
      Association assoc) {

    ParameterList params = new ParameterList();

    params.set(new Parameter("openid.ns", Message.OPENID2_NS));
    params.set(new Parameter("openid.mode", AuthRequest.MODE_IDRES));

    AuthSuccess resp = new GenericAuthSuccess(params);

    resp.setOpEndpoint(opEndpoint);
    resp.setNonce(nonce);
    resp.setReturnTo(returnTo);

    if (invalidateHandle != null) {
      resp.setInvalidateHandle(invalidateHandle);
    }

    resp.setHandle(assoc.getHandle());

    resp.buildSignedList();

    // just for now, so we pass validation...
    resp.setSignature("");

    return resp;
  }


}
