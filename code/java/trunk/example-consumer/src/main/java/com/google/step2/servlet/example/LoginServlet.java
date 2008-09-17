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

package com.google.step2.servlet.example;

import com.google.inject.Inject;
import com.google.step2.AuthRequestHelper;
import com.google.step2.ConsumerHelper;
import com.google.step2.Step2;
import com.google.step2.servlet.InjectableServlet;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.HttpClientPool;
import net.oauth.client.OAuthHttpClient;

import org.apache.commons.httpclient.HttpClient;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class LoginServlet extends InjectableServlet {
  private static final String templateFile = "/WEB-INF/login.jsp";
  private static final String redirectPath =
    "/step2-example-consumer/checkauth";

  private ConsumerHelper consumerHelper;
  private static final String YES_STRING = "yes";

  @Inject
  public void setConsumerHelper(ConsumerHelper helper) {
    this.consumerHelper = helper;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {

    RequestDispatcher d = req.getRequestDispatcher(templateFile);
    d.forward(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    StringBuffer realm = new StringBuffer(req.getScheme());
    realm.append("://").append(req.getServerName());
    realm.append(":").append(req.getServerPort());

    // posted means they're sending us an OpenID4
    String returnToUrl = realm + redirectPath;
    String openId = req.getParameter("openid");
    
    String oauthRequestToken = null;
    
    if (YES_STRING.equals(req.getParameter("oauth"))) { 
      // TODO(sweis): This is just for testing. May have to discover the 
      // token_request URL.
      OAuthServiceProvider provider = new OAuthServiceProvider(
          "http://localhost:9090/oauth-provider/request_token",
          "http://localhost:9090/oauth-provider/authorize",
          "http://localhost:9090/oauth-provider/access_token");

      OAuthConsumer consumer = new OAuthConsumer(
          "http://localhost:8080/oauth", "myKey", "mySecret", provider);
      OAuthAccessor accessor = new OAuthAccessor(consumer);
    
      try {
        new OAuthHttpClient(
            new HttpClientPool() {
              // This trivial 'pool' simply allocates a new client every time.
              // More efficient implementations are possible.
              public HttpClient getHttpClient(URL server) {
                return new HttpClient();
              }}).getRequestToken(accessor);
      } catch (OAuthException e) {
        throw new ServletException(e);
      } catch (URISyntaxException e) {
        throw new ServletException(e);
      }
   
      if (accessor.requestToken != null && accessor.tokenSecret != null) { 
        oauthRequestToken = "oauth_token=" + accessor.requestToken +
        "&oauth_token_secret=" + accessor.tokenSecret;
      }
    }

    AuthRequestHelper helper = consumerHelper.getAuthRequestHelper(
        openId, returnToUrl, oauthRequestToken);
    
    if (YES_STRING.equals(req.getParameter("oauth"))) {
      helper.requestOauthAuthorization();
    }

    if (YES_STRING.equals(req.getParameter("email"))) {
      helper.requestAxAttribute("email", Step2.AX_EMAIL_SCHEMA, true);
    }
    
    if (YES_STRING.equals(req.getParameter("country"))) {
      helper.requestAxAttribute("country", Step2.AX_COUNTRY_SCHEMA, true);
    }

    try {
      AuthRequest authReq = helper.generateRequest();
      authReq.setRealm(realm + "/step2-example-consumer/checkauth");

      HttpSession session = req.getSession();
      session.setAttribute("discovered", helper.getDiscoveryInformation());

      if (YES_STRING.equals(req.getParameter("usePost"))) {
        // using POST
        req.setAttribute("message", authReq);
        RequestDispatcher d =
          req.getRequestDispatcher("/WEB-INF/formredirection.jsp");
        d.forward(req, resp);
      } else {
        // using GET
        resp.sendRedirect(authReq.getDestinationUrl(true));
      }
    } catch (DiscoveryException e) {
      throw new ServletException(e);
    } catch (MessageException e) {
      throw new ServletException(e);
    } catch (ConsumerException e) {
      throw new ServletException(e);
    }
  }
}
