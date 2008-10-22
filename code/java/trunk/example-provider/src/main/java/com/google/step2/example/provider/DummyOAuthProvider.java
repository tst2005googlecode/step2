package com.google.step2.example.provider;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.concurrent.ConcurrentHashMap;

public class DummyOAuthProvider {
  private static final OAuthConsumer CONSUMER =
    new OAuthConsumer(null, "DummyConsumer", "DummySecret", null);

  private static final ConcurrentHashMap<String, OAuthAccessor>
    ACCESSORS = new ConcurrentHashMap<String, OAuthAccessor>();
  
  public static OAuthConsumer getConsumer(String consumerKey) {
    // Just one consumer for now
    return CONSUMER;
  }
  
  public static void putAccessor(String token, OAuthAccessor accessor) {
    ACCESSORS.putIfAbsent(token, accessor);
  }
  
  public static OAuthAccessor getAccessor(String token) {
    return ACCESSORS.get(token);
  }

  public static void removeAccessor(String token) {
    ACCESSORS.remove(token);
  }
  
  public static synchronized void authorizeAccessor(String token)
      throws OAuthException {
    if (token == null) {
      throw new OAuthException("Null token");
    }
    OAuthAccessor accessor = ACCESSORS.remove(token);
    if (accessor == null) {
      throw new OAuthException("Unknown request token");
    }
    accessor.setProperty("authorized", Boolean.TRUE);
    putAccessor(token, accessor);
  }
  
  public static void generateRequestToken(OAuthAccessor accessor) {
    // Request Token is Sha1(consumerKey || current time)
    String consumer_key = (String) accessor.consumer.getProperty("name");
    // generate token and secret based on consumer_key
      
    String tokenData = consumer_key + System.nanoTime();
    accessor.requestToken = DigestUtils.shaHex(tokenData);    
    // Token secret is Sha1(current tiem || Request Token)

    accessor.tokenSecret =
      DigestUtils.shaHex(tokenData + accessor.requestToken);
    accessor.accessToken = null;

    // Cache accessor locally
    putAccessor(accessor.requestToken, accessor);
  }
  
  public static OAuthAccessor generateAccessToken(String requestToken)
      throws OAuthException {
    if (requestToken == null) {
      throw new OAuthException("Null token");
    }

    OAuthAccessor accessor = ACCESSORS.remove(requestToken);
    if (accessor == null) {
      throw new OAuthException("Unknown request token");
    }
    if (accessor.getProperty("authorized") == Boolean.FALSE) {
      putAccessor(requestToken, accessor);
      throw new OAuthException("Request token is not authorized");
    }
    
    String consumerKey = (String) accessor.consumer.getProperty("name");

    // Access token is Sha1(consumerKey || current time)
    String tokenData = consumerKey + System.nanoTime();
    accessor.requestToken = null;
    accessor.accessToken = DigestUtils.shaHex(tokenData);
    accessor.tokenSecret =
      DigestUtils.shaHex(tokenData + accessor.accessToken);

    // Cache accessor locally
    putAccessor(accessor.accessToken, accessor);
    return accessor;
  }
}
