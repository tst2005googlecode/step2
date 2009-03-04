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

package com.google.step2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.ParameterList;

import javax.servlet.http.HttpServletRequest;

/**
 * Step2
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class Step2 {
  private static Log log = LogFactory.getLog(Step2.class);
  
  public enum AxSchema {
    EMAIL("http://axschema.org/contact/email", "email"),
    COUNTRY("http://axschema.org/contact/country/home", "country"),
    LANGUAGE("http://axschema.org/pref/language", "language"),
    FIRST_NAME("http://axschema.org/namePerson/first", "firstName"),
    LAST_NAME("http://axschema.org/namePerson/last", "lastName");
    
    private final String uri;
    private final String shortName;
    
    private AxSchema(String uri, String shortName) {
      this.uri = uri;
      this.shortName = shortName;
    }
    
    public String getUri() {
      return uri;
    }
    
    public String getShortName() {
      return shortName;
    }
  }
  
  /**
   * Returns the URL of an incoming HTTP request, including query parameters.
   * @param req the incoming HTTP request
   * @return the URL that is represented by this HTTP request
   */
  public static String getUrlWithQueryString(HttpServletRequest req) {
    StringBuffer receivingUrl = req.getRequestURL();
    String queryString = req.getQueryString();
    if (queryString != null && queryString.length() > 0) {
      receivingUrl.append("?").append(req.getQueryString());
    }
    log.info(receivingUrl.toString());
    return receivingUrl.toString();
  }

  /**
   * Returns a ParameterList (list of openid-related query parameters) from
   * an HttpServletRequest.
   * @param req the HttpServletRequest
   * @return a ParameterList
   */
  public static ParameterList getParameterList(HttpServletRequest req) {
    return new ParameterList(req.getParameterMap());
  }
}
