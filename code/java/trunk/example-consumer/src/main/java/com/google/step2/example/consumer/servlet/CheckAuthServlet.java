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
import com.google.step2.AuthResponseHelper;
import com.google.step2.ConsumerHelper;
import com.google.step2.Step2;
import com.google.step2.VerificationException;
import com.google.step2.servlet.InjectableServlet;

import org.openid4java.association.AssociationException;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class CheckAuthServlet extends InjectableServlet {
  private ConsumerHelper helper;

  @Inject
  public void setConsumerHelper(ConsumerHelper helper) {
    this.helper = helper;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession();

    try {
      ParameterList openidResp = Step2.getParameterList(req);
      String receivingUrl = Step2.getUrlWithQueryString(req);
      DiscoveryInformation discovered =
        (DiscoveryInformation) session.getAttribute("discovered");
      AuthResponseHelper authResponse = helper.verify(receivingUrl, openidResp,
          discovered);

      Identifier claimedId = authResponse.getClaimedId();
      String email = authResponse.getAxAttributeValue("email");
      String country = authResponse.getAxAttributeValue("country");

      session.setAttribute("user",
          (claimedId == null) ? "unknown" : claimedId.getIdentifier());
      session.setAttribute("email", (email == null) ? "unknown" : email);
      session.setAttribute("country", (country == null) ? "unknown" : country);

    } catch (MessageException e) {
      throw new ServletException(e);
    } catch (DiscoveryException e) {
      throw new ServletException(e);
    } catch (AssociationException e) {
      throw new ServletException(e);
    } catch (VerificationException e) {
      throw new ServletException(e);
    }
    resp.sendRedirect(req.getRequestURI().replaceAll("/checkauth$", "/hello"));
  }
}
