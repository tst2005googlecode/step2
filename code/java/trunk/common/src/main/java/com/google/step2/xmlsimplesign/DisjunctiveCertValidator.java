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

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Matches a certificate if any of the validators supplied to the constructor
 * match the certificate.
 */
public class DisjunctiveCertValidator implements CertValidator {

  private final List<CertValidator> validators;

  public DisjunctiveCertValidator(CertValidator... validators) {
    this.validators = Arrays.asList(validators);
  }

  public boolean matches(X509Certificate cert, String authority) {
    for (CertValidator validator : validators) {
      if (validator.matches(cert, authority)) {
        return true;
      }
    }
    return false;
  }
}
