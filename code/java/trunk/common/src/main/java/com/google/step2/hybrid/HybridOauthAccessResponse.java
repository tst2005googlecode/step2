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

import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;

import java.util.Arrays;
import java.util.List;

/**
 * Hybrid OAuth Message Extension for Access Request Responses
 * 
 * @author steveweis@google.com (Steve Weis)
 */
public class HybridOauthAccessResponse extends HybridOauthResponse {
  
  private final static List<String> requiredFields = 
    Arrays.asList(new String[] {ACCESS_TOKEN, ACCESS_TOKEN_SECRET});
  
  private final static List<String> optionalFields = 
    Arrays.asList(new String[0]);
  
  public HybridOauthAccessResponse(ParameterList parameters) {
    this.parameters = parameters;
  }
  
  public HybridOauthAccessResponse() {
    super();
  }

  @Override
  boolean isValid() {
    return isValid(requiredFields, optionalFields);
  }

  public void setAccessToken(String oauthAccessToken,
      String oauthAccessTokenSecret) {
    parameters.set(new Parameter(ACCESS_TOKEN, oauthAccessToken));
    parameters.set(new Parameter(ACCESS_TOKEN_SECRET, oauthAccessTokenSecret));
  }
}
