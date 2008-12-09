package com.google.step2.example.consumer;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.HttpClientPool;
import net.oauth.client.OAuthHttpClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for requesting OAuth tokens from accessors
 *
 * @author sweis@google.com (Steve Weis)
 */
public class OAuthConsumerUtil {

  private static Logger log = Logger.getLogger(OAuthConsumerUtil.class);

  public static OAuthAccessor getRequestToken(OAuthAccessor accessor)
      throws IOException, OAuthException, URISyntaxException {
    OAuthAccessor accessorCopy = new OAuthAccessor(accessor.consumer);

    OAuthMessage response = getClient().invoke(accessor,
        accessor.consumer.serviceProvider.requestTokenURL,
        OAuth.newList("scope", accessor.getProperty("scope").toString()));
    log.info("Successfully got OAuth request token");
    accessorCopy.requestToken = response.getParameter("oauth_token");
    accessorCopy.tokenSecret = response.getParameter("oauth_token_secret");
    return accessor;
  }

  public static OAuthAccessor getAccessToken(OAuthAccessor accessor)
      throws IOException, OAuthException, URISyntaxException {
    OAuthAccessor accessorCopy = new OAuthAccessor(accessor.consumer);

    OAuthMessage response = getClient().invoke(accessor,
        accessor.consumer.serviceProvider.accessTokenURL,
        OAuth.newList("oauth_token", accessor.requestToken,
            "scope", accessor.getProperty("scope").toString()));
    log.info("Successfully got OAuth access token");
    accessorCopy.accessToken = response.getParameter("oauth_token");
    accessorCopy.tokenSecret = response.getParameter("oauth_token_secret");
    return accessorCopy;
  }

  private static OAuthHttpClient getClient() {
    return new OAuthHttpClient(
        new HttpClientPool() {
          // This trivial 'pool' simply allocates a new client every time.
          // More efficient implementations are possible.
          public HttpClient getHttpClient(URL server) {
            return new HttpClient();
          }
        }
    );
  }
}
