package com.google.step2.example.consumer;

import com.google.step2.example.consumer.servlet.LoginServlet;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuth.Parameter;
import net.oauth.client.HttpClientPool;
import net.oauth.client.OAuthHttpClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;
import org.openid4java.discovery.Discovery;
import org.openid4java.yadis.YadisResolver;
import org.openid4java.yadis.YadisResult;
import org.openxri.xml.SEPType;
import org.openxri.xml.SEPUri;
import org.openxri.xml.Service;
import org.openxri.xml.XRD;
import org.openxri.xml.XRDS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for reading OAuth consumer properties from a Properties object
 *
 * @author sweis@google.com (Steve Weis)
 */
public class OAuthConsumerUtil {
  private static Logger log = Logger.getLogger(OAuthConsumerUtil.class);

  private static final String REQUEST_XRDS =
    "http://oauth.net/core/1.0/endpoint/request";
  private static final String AUTHORIZE_XRDS =
    "http://oauth.net/core/1.0/endpoint/authorize";
  private static final String ACCESS_XRDS =
    "http://oauth.net/core/1.0/endpoint/access";

  private static final String SERVICE_PROVIDER = "serviceProvider";
  private static final String CONSUMER_KEY = "consumerKey";
  private static final String CONSUMER_SECRET = "consumerSecret";
  private static final String SCOPE = "scope";

  private static String consumerKey;
  private static String consumerSecret;
  private static String scope;

  private OAuthServiceProvider provider;

  private final YadisResolver yadisResolver = new YadisResolver();

  private static final ConcurrentHashMap<String, OAuthAccessor>
    ACCESSORS = new ConcurrentHashMap<String, OAuthAccessor>();

  static {
    try {
      ClassLoader loader = LoginServlet.class.getClassLoader();
      String propertiesFile = "/" +
        LoginServlet.class.getPackage().getName().replace(".", "/")
        + "/consumer.properties";
      InputStream stream = loader.getResourceAsStream(propertiesFile);
      Properties properties = new Properties();
      properties.load(stream);
      String consumerName = properties.getProperty("serviceProvider");
      log.info("Setting up OAuth consumer: " + consumerName);
      String serviceProvider = consumerName + "." + SERVICE_PROVIDER;
      consumerKey = properties.getProperty(consumerName + "." + CONSUMER_KEY);
      consumerSecret =
        properties.getProperty(consumerName + "." + CONSUMER_SECRET);
      scope = properties.getProperty(consumerName + "." + SCOPE);

    } catch (IOException e) {
      // If an exception occurs, then no Oauth service provider properties will
      // be read from disk. This will cause an exception below, but won't
      // affect the OpenID authentication
    }
  }

  public static ConcurrentHashMap<String, OAuthServiceProvider> CACHED_DISCOVERED =
    new ConcurrentHashMap<String, OAuthServiceProvider>();

  public OAuthConsumerUtil(String url) {
    provider = CACHED_DISCOVERED.get(url);
    if (provider == null) {
      String requestTokenUrl = null;
      String authorizeUrl = null;
      String accessTokenUrl = null;

      YadisResult yadisResult = yadisResolver.discover(url);
      if (YadisResult.OK == yadisResult.getStatus()) {
        XRD xrd = yadisResult.getXrds().getFinalXRD();
        Service requestService = xrd.getFirstServiceByType(REQUEST_XRDS);
        if (requestService != null && requestService.getNumURIs() > 0) {
          requestTokenUrl =
            requestService.getURIAt(0).getURI().toASCIIString();
        }

        Service authorizeService = xrd.getFirstServiceByType(AUTHORIZE_XRDS);
        if (authorizeService != null && authorizeService.getNumURIs() > 0) {
          authorizeUrl =
            authorizeService.getURIAt(0).getURI().toASCIIString();
        }

        Service accessService = xrd.getFirstServiceByType(ACCESS_XRDS);
        if (accessService != null && accessService.getNumURIs() > 0) {
          accessTokenUrl = accessService.getURIAt(0).getURI().toASCIIString();
        }
      }
      provider = new OAuthServiceProvider(
          requestTokenUrl, authorizeUrl, accessTokenUrl);
      CACHED_DISCOVERED.putIfAbsent(url, provider);
    }
  }

  public OAuthAccessor getAccessToken(String requestToken) {
    if (provider.accessTokenURL != null) {
      OAuthAccessor accessor = ACCESSORS.remove(requestToken);
      if (accessor != null) {
        try {
          OAuthMessage response = getClient().invoke(accessor,
              provider.accessTokenURL,
              OAuth.newList("oauth_token", requestToken));
          log.info("Successfully got OAuth access token");
          accessor.requestToken = null;
          accessor.accessToken = response.getParameter("oauth_token");
          accessor.tokenSecret = response.getParameter("oauth_token_secret");
          ACCESSORS.putIfAbsent(accessor.accessToken, accessor);
          return accessor;
        } catch (OAuthException e) {
          e.printStackTrace();  // Continue
        } catch (IOException e) {
          e.printStackTrace();  // Continue
        } catch (URISyntaxException e) {
          e.printStackTrace();  // Continue
        }
        ACCESSORS.putIfAbsent(requestToken, accessor);
      }
    }
    return null;
  }

  public OAuthServiceProvider getProvider() {
    return provider;
  }

  private OAuthConsumer getConsumer() {
    return new OAuthConsumer(null,  // no callback
        consumerKey, consumerSecret, provider);
  }

  private OAuthAccessor getAccessor() {
    return new OAuthAccessor(getConsumer());
  }

  private OAuthHttpClient getClient() {
    return new OAuthHttpClient(
      new HttpClientPool() {
        // This trivial 'pool' simply allocates a new client every time.
        // More efficient implementations are possible.
        public HttpClient getHttpClient(URL server) {
          return new HttpClient();
        }}
      );
  }
}
