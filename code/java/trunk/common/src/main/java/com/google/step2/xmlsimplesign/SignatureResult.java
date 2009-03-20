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

/**
 * A class that represents the result of signing an XML document, i.e.,
 * the document that was signed, the bits of the signature, and the location
 * of the signature.
 */
public class SignatureResult {

  private final byte[] document;
  private final String signatureLocation;
  private final String signature;

  public SignatureResult(byte[] document, String signatureLocation, String signature) {
    this.document = document;
    this.signatureLocation = signatureLocation;
    this.signature = signature;
  }

  /**
   * Returns the documents that was signed.
   */
  public byte[] getDocument() {
    return document;
  }

  /**
   * Returns the location from where the signature should be served. This is
   * a URL. If we return null here, this means that the signature should be
   * served in the Signature: HTTP header.
   * @return a URL or null
   */
  public String getSignatureLocation() {
    return signatureLocation;
  }

  /**
   * Returns the signature on the document
   */
  public String getSignature() {
    return signature;
  }
}
