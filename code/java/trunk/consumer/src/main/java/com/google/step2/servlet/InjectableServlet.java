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

package com.google.step2.servlet;

import com.google.inject.Injector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class InjectableServlet extends HttpServlet {

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    ServletContext context = config.getServletContext();
    Injector injector = (Injector)
        context.getAttribute(GuiceServletContextListener.INJECTOR_ATTRIBUTE);

    if (injector == null) {
      throw new ServletException("could not find Guice injector");
    }
    injector.injectMembers(this);
  }
}
