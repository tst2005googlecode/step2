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
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple OAuth Request Token Servlet
 * 
 * @author steveweis@gmail.com (Steve Weis)
 */
public class DummyOAuthRequestTokenServlet extends InjectableServlet {
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
    String consumerKey = requestMessage.getConsumerKey();
    OAuthConsumer consumer = DummyOAuthProvider.getConsumer(consumerKey);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    try {
      validator.validateMessage(requestMessage, accessor);
    } catch (Exception e) {
      OAuthServlet.handleException(response, e, request.getLocalName());
    }

    DummyOAuthProvider.generateRequestToken(accessor);
    response.setContentType("text/plain");
    OutputStream out = response.getOutputStream();
    
    OAuth.formEncode(OAuth.newList("oauth_token", accessor.requestToken,
        "oauth_token_secret", accessor.tokenSecret), out);
    out.close();
  }
}
