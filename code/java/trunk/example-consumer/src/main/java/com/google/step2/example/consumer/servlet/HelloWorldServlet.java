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

import com.google.step2.servlet.InjectableServlet;

import java.io.IOException;

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
public class HelloWorldServlet extends InjectableServlet {
  private static String templateFile = "/WEB-INF/hello.jsp";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession();

    // if user clicked "logout", force them to the login page by setting
    // user attribute to null
    if (req.getParameter("logout") != null) {
      session.setAttribute("user", null);
    }

    if (session.getAttribute("user") == null) {
      // redirect to login servlet
      resp.sendRedirect(req.getRequestURI().replaceAll("/hello$", "/login"));
    } else {
      // fill in attributes for the JSP template
      req.setAttribute("user", session.getAttribute("user"));
      req.setAttribute("email", session.getAttribute("email"));
      req.setAttribute("country", session.getAttribute("country"));

      RequestDispatcher d = req.getRequestDispatcher(templateFile);
      d.forward(req, resp);
    }
  }
}
