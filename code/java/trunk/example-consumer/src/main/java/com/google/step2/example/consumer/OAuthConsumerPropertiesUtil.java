package com.google.step2.example.consumer;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

import java.util.Properties;

/**
 * Utility class for reading OAuth consumer properties from a Properties object
 * 
 * @author sweis@google.com (Steve Weis)
 */
public class OAuthConsumerPropertiesUtil {
  private static final String BASE_URL = "baseURL";
  private static final String SERVICE_PROVIDER = "serviceProvider";
  private static final String AUTH_URL = "userAuthorizationURL";
  private static final String REQUEST_TOKEN_URL = "requestTokenURL";
  private static final String ACCESS_TOKEN_URL = "accessTokenURL";
  private static final String AUTHORIZE_URL = "userAuthorizationURL";
  private static final String CONSUMER_KEY = "consumerKey";
  private static final String CONSUMER_SECRET = "consumerSecret";

  private final String baseUrl;
  private final String requestTokenUrlSuffix;
  private final String accessTokenUrlSuffix;
  private final String authorizeUrlSuffix;
  private final String consumerKey;
  private final String consumerSecret;
    
  private final OAuthServiceProvider provider;
  
  public OAuthConsumerPropertiesUtil(String consumerName, Properties properties) {  
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

    provider = new OAuthServiceProvider(
        baseUrl + "/" + requestTokenUrlSuffix,
        baseUrl + "/" + authorizeUrlSuffix,
        baseUrl + "/" + accessTokenUrlSuffix);    
  }
  
  public OAuthServiceProvider getProvider() {
    return provider;
  }
  
  public OAuthConsumer getConsumer() {
    return new OAuthConsumer("", // No Callback,
        consumerKey, consumerSecret, provider);
  }
}
