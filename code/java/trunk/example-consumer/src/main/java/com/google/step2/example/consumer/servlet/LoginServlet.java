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

package com.google.step2.example.consumer.servlet;

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
import org.apache.log4j.Logger;
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
  private Logger log = Logger.getLogger(LoginServlet.class); 
  private static final String TEMPLATE_FILE = "/WEB-INF/login.jsp";
  private static final String PROJECT = "/step2-example-consumer";
  private static final String REDIRECT_PATH = "/checkauth";

  private ConsumerHelper consumerHelper;
  private static final String YES_STRING = "yes";

  @Inject
  public void setConsumerHelper(ConsumerHelper helper) {
    this.consumerHelper = helper;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {

    RequestDispatcher d = req.getRequestDispatcher(TEMPLATE_FILE);
    d.forward(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    log.info("Login Servlet Post");
    StringBuffer realm = new StringBuffer(req.getScheme());
    realm.append("://").append(req.getServerName());
    realm.append(":").append(req.getServerPort());
    
    // posted means they're sending us an OpenID4
    String returnToUrl = realm + PROJECT + REDIRECT_PATH;
    String openId = req.getParameter("openid");
    
    String oauthRequestToken = null;
    
    if (YES_STRING.equals(req.getParameter("oauth"))) {
      log.info("Oauth Request");
      // TODO(sweis): This is just for testing. Will have to discover the 
      // token_request URL.
      String dummyProvider = req.getScheme() + "://" + req.getServerName() +
        ":8081" + "/step2-example-provider";
      OAuthServiceProvider provider = new OAuthServiceProvider(
          dummyProvider + "/request_token",
          dummyProvider + "/authorize",
          dummyProvider + "/access_token");

      OAuthConsumer consumer = new OAuthConsumer("",  // No Callback
          "DummyConsumer", "DummySecret", provider);
      OAuthAccessor accessor = new OAuthAccessor(consumer);
    
      try {
        OAuthHttpClient client = new OAuthHttpClient(
            new HttpClientPool() {
              // This trivial 'pool' simply allocates a new client every time.
              // More efficient implementations are possible.
              public HttpClient getHttpClient(URL server) {
                return new HttpClient();
              }}
            );
        client.getRequestToken(accessor);
        log.info("Successful Oauth token request: " + accessor.toString());
      } catch (OAuthException e) {
        //throw new ServletException(e);
        e.printStackTrace();  // Continue
      } catch (URISyntaxException e) {
        //throw new ServletException(e);
        e.printStackTrace();  // Continue
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
