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

import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;

/**
 * Temporary fix to enable more flexible request handling than openid4java
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class AxMessage2 extends AxMessage {
  public static final String OPENID_NS_AX_FINAL =
      "http://openid.net/srv/ax/1.0";

  static {
  }
  
  @Override
  public String getTypeUri() {
    return OPENID_NS_AX_FINAL;
  }
  
  public AxMessage2() {
    super();
  }
  
  public AxMessage2(ParameterList params) {
    super(params);
  }
  
  /**
   * The openid4java library will only allow omission of the claimed_id and
   * identity attributes from requests and responses if some other extension
   * included in the message claims to provide an alternative "identifier".
   */
  @Override
  public boolean providesIdentifier() {
    return true;
  }

  @Override
  public MessageExtension getExtension(ParameterList parameterList,
      boolean isRequest) throws MessageException {
    String axMode = null;
    if (parameterList.hasParameter("mode")) {
      axMode = parameterList.getParameterValue("mode");

      if ("fetch_request".equals(axMode)) {
        return FetchRequest2.createFetchRequest2(parameterList);
      } else if ("fetch_response".equals(axMode)) {
        return FetchResponse2.createFetchResponse2(parameterList);
      } else {
        return super.getExtension(parameterList, isRequest);
      }
    }

    throw new MessageException("Invalid value for attribute exchange mode: "
        + axMode);
  }
}
