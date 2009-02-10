/**
 * Copyright 2009 Google Inc.
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
import com.google.step2.consumer.OAuthProviderInfoStore;
import com.google.step2.consumer.ProviderInfoNotFoundException;
import com.google.step2.servlet.InjectableServlet;
import com.google.step2.Step2;

import net.oauth.OAuthAccessor;

import org.openid4java.discovery.yadis.YadisException;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.discovery.yadis.YadisResult;
import org.openxri.xml.SEPType;
import org.openxri.xml.SEPUri;
import org.openxri.xml.Service;
import org.openxri.xml.XRD;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Example Servlet that uses a popup UI to log users in using a
 * 'login with Google' button
 *
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 */
public class LoginViaPopupServlet extends InjectableServlet {

  private static final String OPENID_2_0_SERVER = "http://specs.openid.net/auth/2.0/server";
  private static final String UTF8 = "UTF-8";
  private static final String TEMPLATE_FILE = "/WEB-INF/popup.jsp";
  private static final String PROJECT = "";
  private static final String REDIRECT_PATH = "/checkauth?login_type=popup";
  public static final String RETURN_TO = "step2_popup_return_to";
  public static final String REALM = "step2_popup_realm";
  public static final String OP_ENDPOINT= "step2_popup_op_endpoint";
  public static final String BUTTON_IMAGE = "step2_popup_button_image";
  public static final String OP_FRIENDLY = "step2_popup_op_friendly_name";
  public static final String EXTENSION_PARAMS = "step2_popup_extension_params";

  private static final Logger logger =
      Logger.getLogger(LoginViaPopupServlet.class.getCanonicalName());
  private static final String AX_1_0 = "http://openid.net/srv/ax/1.0";

  private OAuthProviderInfoStore providerStore;

  @Inject
  public void setProviderInfoStore(OAuthProviderInfoStore store) {
    this.providerStore = store;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {

    resp.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    resp.setHeader("Pragma", "no-cache");
    resp.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
    resp.setDateHeader("Date", System.currentTimeMillis());
    HttpSession session = req.getSession();

    if (req.getParameter("logout") != null) {
      logoutUser(req);
    }

    // Set our realm; in practice one would make the realm site-wide,
    // for instance, substituting the host part of the domain by '*'
    // This also assumes that you want an https return_to URL if the page
    // where the button is displayed is over https, and http otherwise. In
    // practice, it could well be the case that you want the return_to URL
    // to be https, but the page where the user initially sees the login is
    // over http.
    StringBuffer realm = new StringBuffer(req.getScheme());
    realm.append("://").append(req.getServerName());
    int port = req.getServerPort();
    if (port != 80 && port != 443) {
      realm.append(":").append(port);
    }
    session.setAttribute(REALM, realm.toString());

    // Now set the return_to URL
    realm.append(PROJECT)
        .append(REDIRECT_PATH);
    session.setAttribute(RETURN_TO, realm.toString());

    // Do per-OP customizations:
    perOpCustomize(req);

    RequestDispatcher d = req.getRequestDispatcher(TEMPLATE_FILE);
    d.forward(req, resp);
  }

  private void logoutUser(HttpServletRequest req) {
    HttpSession session = req.getSession();
    session.setAttribute("user", null);

    // Clean up stale session state if any
    for (Step2.AxSchema schema : Step2.AxSchema.values()) {
      session.removeAttribute(schema.getShortName());
    }
    session.removeAttribute("request_token");
    session.removeAttribute("access_token");
    session.removeAttribute("access_token_secret");
    session.removeAttribute("accessor");
  }

  /**
   * Here we are assuming that the OP is always Google Accounts. In practice,
   * an RP could use some technique to identify the user's preferred
   * identity provider.
   * @param req the incoming request
   * @throws ServletException
   */
  @SuppressWarnings("unchecked")
  private void perOpCustomize(HttpServletRequest req) throws ServletException {

    HttpSession session = req.getSession();
    // Now set the endpoint for user authentication.
    Service service = discoverService(OPENID_2_0_SERVER);
    Vector<SEPType> types = service.getTypes();
    String endpointUrl = null;
    for (SEPType type : types) {
      // This RP support Attribute Exchange, so looks if advertized as a supported
      // type for this OpenID server
      if (type.getType().equals(AX_1_0)) {
        session.setAttribute(EXTENSION_PARAMS,
            getExtensionParameters(req.getServerName()));
      }
    }
    // Now for the actual location URL of the service
    Vector<SEPUri> uris = service.getURIs();
    if ((null == uris) || (uris.size() == 0)) {
      throw new ServletException("Service endpoint not found!");
    }
    // Use the first location
    session.setAttribute(OP_ENDPOINT, uris.get(0).getURI().toString());
    session.setAttribute(BUTTON_IMAGE, OpSettings.GOOGLE.getImage());
    session.setAttribute(OP_FRIENDLY, OpSettings.GOOGLE.getFriendlyName());
  }

  /**
   * This assumes that the OP is 'Google Accounts' so it performs discovery
   * at that location. In practice, this needs to be generalized for other OPs.
   * @return the endpoint for user login.
   */
  private Service discoverService(String type) {
    String discoveryUrl = OpSettings.GOOGLE.getDiscoveryUrl();
    // TODO(breno): Add caching of discovery results to this example.
    YadisResolver resolver = new YadisResolver();
    try {
      YadisResult result = resolver.discover(discoveryUrl);
      XRD xrd = result.getXrds().getFinalXRD();
      return xrd.getServiceForType(type);
    } catch (YadisException e) {
      // Can't find the server endpoint
      logger.severe("Can't find the op Enpoint! Yadis exception thrown:"
          + e.getMessage());
      return null;
    }
  }

  private String getExtensionParameters(String consumer) {
    StringBuffer buf =  new StringBuffer()
        .append("{ ")
        .append(getAxExtensionParameters());

    String oauthParams = getOAuthExtensionParameters(consumer);
    if (oauthParams.length() > 0) {
        buf.append(", ")
          .append(getOAuthExtensionParameters(consumer));
    }

    return buf.append(" } ").toString();
  }

  /**
   * Returns an attribute exchange fetch request, in this case asking for
   * email, name, country, language, and an additional web URL.
   */
  private String getAxExtensionParameters() {
    return new StringBuffer("'openid.ns.ax' : 'http://openid.net/srv/ax/1.0', ")
        .append("'openid.ax.mode' : 'fetch_request', ")
        .append("'openid.ax.type.email' : 'http://axschema.org/contact/email', ")
        .append("'openid.ax.type.first' : 'http://axschema.org/namePerson/first', ")
        .append("'openid.ax.type.last' : 'http://axschema.org/namePerson/last', ")
        .append("'openid.ax.type.country' : 'http://axschema.org/contact/country/home', ")
        .append("'openid.ax.type.lang' : 'http://axschema.org/pref/language', ")
        .append("'openid.ax.type.web' : 'http://axschema.org/contact/web/default', ")
        .append("'openid.ax.required' : 'email,first,last,country,lang,web'")
        .toString();
  }

  private String getOAuthExtensionParameters(String consumer) {

    // make sure that the data in the configuration file is actually for
    // this RP.
    OAuthAccessor accessor;
    try {
      accessor = providerStore.getOAuthAccessor("google");
    } catch (ProviderInfoNotFoundException e) {
      return "";
    }

    if (!consumer.equals(accessor.consumer.consumerKey)) {
      return "";
    }

    return new StringBuffer("'openid.ns.oauth' : 'http://specs.openid.net/extensions/oauth/1.0', ")
      .append("'openid.oauth.consumer' : '" + consumer + "', ")
      .append("'openid.oauth.scope' : 'http://www.google.com/m8/feeds/' ")
      .toString();
  }

  private static enum OpSettings {
    // Add customizations here
    GOOGLE("https://www.google.com/accounts/o8/id",
        "Sign in with a Google Account",
        "http://www.google.com/favicon.ico");

    public String getDiscoveryUrl() {
      return discoveryUrl;
    }

    public String getFriendlyName() {
      return friendlyName;
    }

    public String getImage() {
      return image;
    }

    private final String discoveryUrl;
    private final String friendlyName;
    private final String image;

    private OpSettings(String discoveryUrl, String friendlyName, String image) {
      this.discoveryUrl = discoveryUrl;
      this.friendlyName = friendlyName;
      this.image = image;
    }
  }
}
