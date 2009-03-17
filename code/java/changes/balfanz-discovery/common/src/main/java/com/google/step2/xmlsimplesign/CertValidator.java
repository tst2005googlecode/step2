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

import com.google.inject.ImplementedBy;
import java.security.cert.X509Certificate;

/**
 * Interface for validating certificates.
 */
@ImplementedBy(DefaultCertValidator.class)
public interface CertValidator {

  /**
   * Returns true if the certificate matches the given authority.
   * @param cert the certificate in question. This is usually a certificate
   *   that was used to sign an XRD document.
   * @param authority the authority that is supposed to have signed the XRD
   *   document. Usually, this will be the CanonicalID or Subject of the XRD
   *   document, but it might also be an authority explicitly delegated to by
   *   a previous XRD document. In either case, the authority has to "match" the
   *   certificate.
   * @return true, if the authority matches the certificate, false otherwise
   */
  boolean matches(X509Certificate cert, String authority);

}
