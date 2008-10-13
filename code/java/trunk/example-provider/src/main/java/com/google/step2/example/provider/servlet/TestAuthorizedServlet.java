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

import com.google.step2.example.provider.Step2OAuthProvider;
import com.google.step2.servlet.InjectableServlet;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Check whether a token value has been authorized and if it is a request
 * or access token.
 * 
 * @author steveweis@gmail.com (Steve Weis)
 */
public class TestAuthorizedServlet extends InjectableServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
    OAuthAccessor accessor =
      Step2OAuthProvider.getAccessor(requestMessage.getToken());
    String authorized = "false";
    String requestToken = "none";
    String accessToken = "none";
    if (accessor != null) {
      if (accessor.getProperty("authorized") == Boolean.TRUE) {
        authorized = "true";
      }
      if (accessor.requestToken != null) {
        requestToken = accessor.requestToken;
      }
      if (accessor.accessToken != null) {
        accessToken = accessor.accessToken;
      }
    }
    response.setContentType("text/plain");
    OutputStream out = response.getOutputStream();
    
    OAuth.formEncode(OAuth.newList("authorized", authorized, "requestToken",
        requestToken, "accessToken", accessToken), out);
    out.close();
  }
}
