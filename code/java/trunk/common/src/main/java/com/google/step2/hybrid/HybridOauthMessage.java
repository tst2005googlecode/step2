/**
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.google.step2.hybrid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.MessageExtensionFactory;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;

import java.util.List;

/**
 * Hybrid OAuth Message Extension
 *
 * @author steveweis@google.com (Steve Weis)
 */
public class HybridOauthMessage implements MessageExtension,
    MessageExtensionFactory {

  protected ParameterList parameters = new ParameterList();

  public static final String OPENID_NS_OAUTH =
    "http://specs.openid.net/extensions/oauth/1.0";

  static final String SCOPE = "scope";
  static final String OAUTH_TOKEN = "request_token";

  @SuppressWarnings("unused")
  private static Log log = LogFactory.getLog(HybridOauthMessage.class);

  public MessageExtension getExtension(ParameterList parameterList,
      boolean isRequest) throws MessageException {
    if (isRequest) {
      return new HybridOauthRequest(parameterList);
    } else {
      return new HybridOauthResponse(parameterList);
    }
  }

  public String getParameter(String name) {
    return parameters.getParameterValue(name);
  }

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

  public boolean signRequired() {
    return true;
  }

  protected boolean isValid(List<String> requiredFields,
      List<String> optionalFields) {
    // Ensure that all required fields are in this request
    for (String required : requiredFields) {
      if (!parameters.hasParameter(required)) {
        return false;
      }
    }

    // Check that all fields in this request are required or optional
    @SuppressWarnings("unchecked")
    List<Parameter> params = parameters.getParameters();
    for (Parameter p : params) {
      if (!requiredFields.contains(p.getKey()) &&
          !optionalFields.contains(p.getKey())) {
        return false;
      }
    }

    return true;
  }

  public String getScope() {
    return parameters.getParameterValue(SCOPE);
  }
}
