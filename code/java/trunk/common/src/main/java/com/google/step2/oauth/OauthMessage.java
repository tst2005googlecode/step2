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

package com.google.step2.oauth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.MessageExtensionFactory;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;

/**
 * 
 * 
 * @author steveweis@google.com (Steve Weis)
 */
public class OauthMessage implements MessageExtension, MessageExtensionFactory {

  private static Log log = LogFactory.getLog(OauthMessage.class);

  protected ParameterList parameters = new ParameterList();

  public static final String OPENID_NS_OAUTH =
    "http://specs.openid.net/extensions/oauth/1.0";
  
  public ParameterList getParameters() {
    return parameters;
  }

  public String getTypeUri() {
    return OPENID_NS_OAUTH;
  }

  public boolean providesIdentifier() {
    return false;
  }

  public void setParameters(ParameterList params) {
    parameters = params;
  }

  public boolean hasParameter(String name) {
    return parameters.hasParameter(name);
  }

  public String getParameter(String name) {
    return parameters.getParameterValue(name);
  }

  public MessageExtension getExtension(ParameterList parameterList,
      boolean isRequest) throws MessageException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean signRequired() {
    return true;
  }
  
}
