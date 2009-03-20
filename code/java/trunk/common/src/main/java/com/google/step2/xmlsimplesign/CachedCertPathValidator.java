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

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.step2.util.ExpiringLruCache;
import com.google.step2.util.TimeSource;

/**
 * Verifies X.509 certificates.
 *
 * This makes heavy use of the JRE CertPath libraries.  Documentation may be found at
 * http://java.sun.com/j2se/1.5.0/docs/guide/security/certpath/CertPathProgGuide.html
 *
 * TODO: look for sane PKIXParameter certificate policy configuration.
 */
public class CachedCertPathValidator {

  private static final Logger log = Logger.getLogger(CachedCertPathValidator.class.getName());
  private static final String VALIDATOR_TYPE = "PKIX";
  private static final String CERTIFICATE_TYPE = "X.509";
  private static final int VALIDATION_CACHE_SIZE = 1024;
  private static final long VALIDATION_CACHE_AGE_SECONDS = 10 * 60;

  private final Set<TrustAnchor> trustRoots;
  private final ExpiringLruCache<List<X509Certificate>, Boolean> validationCache;

  private TimeSource timeSource = new TimeSource();

  @Inject
  public CachedCertPathValidator(TrustRootsProvider trustRoots) {
    this(trustRoots.getTrustRoots());
  }

  /* visible for testing */
  public CachedCertPathValidator(Collection<X509Certificate> trustRoots) {
    this.trustRoots = createTrustRoots(trustRoots);
    this.validationCache = new ExpiringLruCache<List<X509Certificate>, Boolean>(
        VALIDATION_CACHE_SIZE);
  }

  private ImmutableSet<TrustAnchor> createTrustRoots(Collection<X509Certificate> trustRoots) {
    List<TrustAnchor> anchors = Lists.newArrayList();
    for (X509Certificate c : trustRoots) {
      anchors.add(new TrustAnchor(c, null));
    }
    return ImmutableSet.copyOf(anchors);
  }

  public void setTimeSource(TimeSource timeSource) {
    this.timeSource = timeSource;
    validationCache.setTimeSource(timeSource);
  }

  public void validate(List<X509Certificate> certs) throws CertValidatorException {
    // If a cert chain validates successfully, we cache it for several minutes.  This improves
    // performance dramatically (anywhere from 10x to 50x decrease in CPU usage when repeatedly
    // verifying the same certificate chain.
    if (validationCache.get(certs) != null) {
      return;
    }
    validateNoCache(certs);
    validationCache.put(certs, Boolean.TRUE, VALIDATION_CACHE_AGE_SECONDS);
  }

  private void validateNoCache(List<X509Certificate> certs) throws CertValidatorException {
    try {
      CertPathValidator validator = CertPathValidator.getInstance(VALIDATOR_TYPE);
      PKIXParameters params = new PKIXParameters(trustRoots);
      params.setDate(timeSource.now());
      params.setRevocationEnabled(false);
      CertificateFactory certFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
      CertPath certPath = certFactory.generateCertPath(certs);
      validator.validate(certPath, params);
    } catch (GeneralSecurityException e) {
      log.log(Level.WARNING, "Certificate validation failed, certs were: " + certs, e);
      throw new CertValidatorException("Certificate validation failure", e);
    }
  }
}
