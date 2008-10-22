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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for reading OAuth consumer properties from a Properties object
 * 
 * @author sweis@google.com (Steve Weis)
 */
public class OAuthConsumerUtil {
  private Logger log = Logger.getLogger(OAuthConsumerUtil.class);
  
  private static final String BASE_URL = "baseURL";
  private static final String SERVICE_PROVIDER = "serviceProvider";
  private static final String AUTH_URL = "userAuthorizationURL";
  private static final String REQUEST_TOKEN_URL = "requestTokenURL";
  private static final String ACCESS_TOKEN_URL = "accessTokenURL";
  private static final String AUTHORIZE_URL = "userAuthorizationURL";
  private static final String CONSUMER_KEY = "consumerKey";
  private static final String CONSUMER_SECRET = "consumerSecret";
  private static final String SCOPE = "scope";
  
  private final String baseUrl;
  private final String requestTokenUrlSuffix;
  private final String accessTokenUrlSuffix;
  private final String authorizeUrlSuffix;
  private final String consumerKey;
  private final String consumerSecret;
  private final String scope;
    
  private final OAuthServiceProvider provider;
  private final OAuthConsumer consumer;
  
  private static final ConcurrentHashMap<String, OAuthAccessor>
  ACCESSORS = new ConcurrentHashMap<String, OAuthAccessor>();
  
  private final static String OAUTH_PROPERTIES_FILE = "/" +
  LoginServlet.class.getPackage().getName().replace(".", "/")
  + "/consumer.properties";

  private final static Properties PROPERTIES = new Properties();
  
  static {
    try {
      ClassLoader loader = LoginServlet.class.getClassLoader();
      InputStream stream = loader.getResourceAsStream(OAUTH_PROPERTIES_FILE);
      PROPERTIES.load(stream);
    } catch (IOException e) {
      // If an exception occurs, then no Oauth service provider properties will
      // be read from disk. This will cause an exception below, but won't
      // affect the OpenID authentication
    }
  }

  public final static OAuthConsumerUtil DEFAULT = new OAuthConsumerUtil(
      PROPERTIES.getProperty("serviceProvider"), PROPERTIES);

  public OAuthConsumerUtil(String consumerName, Properties properties) {
    log.info("Setting up OAuth consumer: " + consumerName);
    String serviceProvider = consumerName + "." + SERVICE_PROVIDER;
    baseUrl = properties.getProperty(serviceProvider + "." + BASE_URL);
    requestTokenUrlSuffix =
      properties.getProperty(serviceProvider + "." + REQUEST_TOKEN_URL);
    accessTokenUrlSuffix =
      properties.getProperty(serviceProvider + "." + ACCESS_TOKEN_URL);
    authorizeUrlSuffix = 
      properties.getProperty(serviceProvider + "." + AUTHORIZE_URL);
    consumerKey = properties.getProperty(consumerName + "." + CONSUMER_KEY);
    consumerSecret =
      properties.getProperty(consumerName + "." + CONSUMER_SECRET);
    scope = properties.getProperty(consumerName + "." + SCOPE);
    
    provider = new OAuthServiceProvider(
        baseUrl + "/" + requestTokenUrlSuffix,
        baseUrl + "/" + authorizeUrlSuffix,
        baseUrl + "/" + accessTokenUrlSuffix);    
    consumer = new OAuthConsumer("", // No Callback,
        consumerKey, consumerSecret, provider);
  }
    
  public String getUnauthorizedRequestToken() {
    OAuthAccessor accessor = getAccessor();
    Collection<OAuth.Parameter> parameters = null;
    if (scope != null) {
      parameters = new ArrayList<OAuth.Parameter>();
      parameters.add(new Parameter(SCOPE, scope));
    }

    try {
      OAuthMessage response = 
        getClient().invoke(accessor, provider.requestTokenURL, parameters);
      log.info("Successfully got OAuth request token");
      accessor.requestToken = response.getParameter("oauth_token");
      accessor.tokenSecret = response.getParameter("oauth_token_secret");
      ACCESSORS.put(accessor.requestToken, accessor);
      return response.getToken();
    } catch (OAuthException e) {
      e.printStackTrace();  // Continue
    } catch (URISyntaxException e) {
      e.printStackTrace();  // Continue
    } catch (IOException e) {
      e.printStackTrace();  // Continue
    }
    return null;
  }
    
  public OAuthAccessor getAccessToken(String requestToken) {
    OAuthAccessor accessor = ACCESSORS.remove(requestToken);
    if (accessor != null) {
      try {
        OAuthMessage response = getClient().invoke(accessor,
            consumer.serviceProvider.accessTokenURL,
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
    return null;
  }
  
  public OAuthServiceProvider getProvider() {
    return new OAuthServiceProvider(
        baseUrl + "/" + requestTokenUrlSuffix,
        baseUrl + "/" + authorizeUrlSuffix,
        baseUrl + "/" + accessTokenUrlSuffix);
  }
  
  private OAuthConsumer getConsumer() {
    return consumer;
  }
  
  private OAuthAccessor getAccessor() {
    return new OAuthAccessor(consumer);
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
