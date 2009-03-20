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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A cert validator that will match the given cert if and only if its
 * subject's Common Name equals a given string.
 */
public abstract class CnConstraintCertValidator implements CertValidator {

  private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

  public boolean matches(X509Certificate cert, String authority) {
    String cn = getCnFromDn(cert.getSubjectX500Principal().getName());

    if (cn == null) {
      return false;
    }

    return cn.equals(getRequiredCn(authority));
  }

  /* visible for testing */
  String getCnFromDn(String dn) {
    Matcher m = CN_PATTERN.matcher(dn);

    if (m.find()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  /**
   * Returns the string that has to equal the cert's subject's CN.
   * @param authority usually, the canonical ID of the XRD that we're currently
   *   validating; but could be the name of an explicitly delegated-to
   *   authority. A subclass could for example implement the following:
   *   if the authority is a full URL, it returns here only the name of the
   *   host in the URL, since X.509 certificates aren't usually issued to
   *   URLs for DNS host names.
   */
  protected abstract String getRequiredCn(String authority);
}
