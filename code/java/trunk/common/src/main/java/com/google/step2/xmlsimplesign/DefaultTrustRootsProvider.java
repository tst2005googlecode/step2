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
package com.google.step2.xmlsimplesign;

import com.google.inject.Singleton;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * By default, we use the same CA roots that Java's SSL implementation uses.
 */
@Singleton
public class DefaultTrustRootsProvider implements TrustRootsProvider {

  private final Collection<X509Certificate> roots;

  public DefaultTrustRootsProvider() {

    TrustManagerFactory factory;
    try {
      factory = TrustManagerFactory.getInstance("X509");

      // null keystore means we load the keystore defined through
      // -Djavax.net.ssl.trustStore, or if that is not set, a keystore on a
      // default path
      factory.init((KeyStore) null);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    } catch (KeyStoreException e) {
      throw new IllegalStateException(e);
    }
    TrustManager[] managers = factory.getTrustManagers();

    // there is only one trust manager for the SSL root certs
    X509TrustManager manager = (X509TrustManager) managers[0];

    this.roots = Arrays.asList(manager.getAcceptedIssuers());
  }

  public Collection<X509Certificate> getTrustRoots() {
    return roots;
  }
}
