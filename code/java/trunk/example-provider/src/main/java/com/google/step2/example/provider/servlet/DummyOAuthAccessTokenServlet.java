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

package com.google.step2.example.provider.servlet;

import com.google.step2.example.provider.DummyOAuthProvider;
import com.google.step2.servlet.InjectableServlet;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple OAuth Access Token Servlet
 * 
 * @author steveweis@gmail.com (Steve Weis)
 */
public class DummyOAuthAccessTokenServlet extends InjectableServlet {
  private static final SimpleOAuthValidator validator =
    new SimpleOAuthValidator();
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    
    OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
    String requestToken = requestMessage.getToken();
    OAuthAccessor accessor = DummyOAuthProvider.getAccessor(requestToken);
    boolean valid = false;
    
    try {
      validator.validateMessage(requestMessage, accessor);
      valid = true;
    } catch (Exception e) {
      OAuthServlet.handleException(response, e, request.getLocalName());
    }

    response.setContentType("text/plain");
    OutputStream out = response.getOutputStream();
    if (valid) {
      // Check that the request token has been authorized
      if (!Boolean.TRUE.equals(accessor.getProperty("authorized"))) {
        OAuthServlet.handleException(response,
            new OAuthProblemException("permission_denied"),
            request.getLocalName());
      } else {
        try {
          DummyOAuthProvider.generateAccessToken(requestToken);
          OAuth.formEncode(OAuth.newList("oauth_token", accessor.accessToken,
              "oauth_token_secret", accessor.tokenSecret), out);
        } catch (OAuthException e) {
          OAuthServlet.handleException(response, e, request.getLocalName());
        }     
      }
    }

    out.close();
  }
}
