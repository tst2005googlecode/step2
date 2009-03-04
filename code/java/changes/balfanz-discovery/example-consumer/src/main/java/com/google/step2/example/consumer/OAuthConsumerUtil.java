package com.google.step2.example.consumer;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Utility class for requesting OAuth tokens from accessors
 *
 * @author sweis@google.com (Steve Weis)
 */
public class OAuthConsumerUtil {

  private static Log log = LogFactory.getLog(OAuthConsumerUtil.class);

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

  private static OAuthClient getClient() {
    return new OAuthClient(new HttpClient4());
  }
}
