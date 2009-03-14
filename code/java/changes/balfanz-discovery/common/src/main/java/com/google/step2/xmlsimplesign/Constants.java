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

import org.jdom.Namespace;

/**
 * Some constants we're using.
 */
public class Constants {

  public static final String XML_DSIG_NAMESPACE = "http://www.w3.org/2000/09/xmldsig#";
  public static final String SIMPLE_SIGN_NAMESPACE = "http://docs.oasis-open.org/xri/xrd/2009/01";

  public static final String SIGNATURE_ELEMENT = "Signature";
  public static final String SIGNED_INFO_ELEMENT = "SignedInfo";
  public static final String ALGORITHM_ATTRIBUTE = "Algorithm";
  public static final String CANONICALIZATION_METHOD_ELEMENT = "CanonicalizationMethod";
  public static final String SIGNATURE_METHOD_ELEMENT = "SignatureMethod";
  public static final String SIGNATURE_LOCATION_ELEMENT = "SignatureLocation";
  public static final String KEY_INFO_ELEMENT = "KeyInfo";
  public static final String X509_DATA_ELEMENT = "X509Data";
  public static final String X509_CERTIFICATE = "X509Certificate";

  public static final String RSA_SHA1_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

  public static final String CANONICALIZE_RAW_OCTETS =
      "http://docs.oasis-open.org/xri/xrd/2009/01#canonicalize-raw-octets";



  static final Namespace XML_DSIG_NS = Namespace.getNamespace("ds", XML_DSIG_NAMESPACE);
  static final Namespace SIMPLE_SIGN_NS = Namespace.getNamespace("sds", SIMPLE_SIGN_NAMESPACE);

  static final String RSA_SHA1_JCE_ID = "SHA1withRSA";
}
