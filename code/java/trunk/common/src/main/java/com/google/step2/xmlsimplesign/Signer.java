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

import com.google.step2.util.EncodingUtil;
import com.google.step2.util.Preconditions;
import com.google.step2.util.RandUtil;
import com.google.step2.util.XmlUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;


/**
 * Signs XML documents.
 */
public class Signer {

  private byte[] document;
  private PrivateKey privateKey;
  private X509Certificate signingCert;
  private List<X509Certificate> certificateChain = new ArrayList<X509Certificate>();
  private String signatureFilePrefix;

  public Signer() {
  }

  public Signer setDocument(byte[] document) {
    this.document = document;
    return this;
  }

  public Signer setSigningKey(X509Certificate signingCert, PrivateKey privateKey) {
    this.signingCert = signingCert;
    this.privateKey = privateKey;
    return this;
  }

  /**
   * Sets the prefix of the location from which signatures should be served.
   * @param signatureFilePrefix if null, this means no SignatureLocation element
   *   will be included in the signed document. Rather, the signature is assumed
   *   to be in the HTTP Signature: header.
   * @return this Signer
   */
  public Signer setSignatureFilePrefix(String signatureFilePrefix) {
    this.signatureFilePrefix = signatureFilePrefix;
    return this;
  }

  public Signer addIntermediateCert(X509Certificate intermediate) {
    certificateChain.add(intermediate);
    return this;
  }

  public SignatureResult sign() throws XmlSimpleSignException {
    Preconditions.checkNotNull(document);
    Preconditions.checkNotNull(signingCert);
    Preconditions.checkNotNull(certificateChain);

    try {
      String signatureLocation = (signatureFilePrefix == null)
        ? null
        : signatureFilePrefix + RandUtil.getRandomString(8);
      Document xml = XmlUtil.getJdomDocument(new ByteArrayInputStream(document));
      xml.getRootElement().addContent(0, createSignatureElement(signatureLocation));

      XMLOutputter outputter = new XMLOutputter();
      outputter.setFormat(Format.getPrettyFormat());
      String textDoc = outputter.outputString(xml);
      byte[] docBytes = EncodingUtil.getUtf8Bytes(textDoc);
      String sig = signDoc(docBytes);
      return new SignatureResult(docBytes, signatureLocation, sig);
    } catch (JDOMException e) {
      throw new XmlSimpleSignException("Can't parse input XML", e);
    } catch (IOException e) {
      throw new XmlSimpleSignException("Can't parse input XML", e);
    } catch (GeneralSecurityException e) {
      throw new XmlSimpleSignException("Can't sign document", e);
    }
  }

  private Element createSignatureElement(String location) throws GeneralSecurityException {
    Element sig = new Element(Constants.SIGNATURE_ELEMENT, Constants.XML_DSIG_NS);

    Element signedInfo = dsigElement(Constants.SIGNED_INFO_ELEMENT);

    Element c14n = dsigElement(Constants.CANONICALIZATION_METHOD_ELEMENT);
    c14n.setAttribute(Constants.ALGORITHM_ATTRIBUTE,
        Constants.CANONICALIZE_RAW_OCTETS);

    // TODO: add support for rsa-sha256 and dsa
    Element signatureMethod = dsigElement(Constants.SIGNATURE_METHOD_ELEMENT);
    signatureMethod.setAttribute(Constants.ALGORITHM_ATTRIBUTE,
        Constants.RSA_SHA1_ALGORITHM);

    Element signatureLocation = null;
    if (location != null) {
      signatureLocation = simpleSigElement(Constants.SIGNATURE_LOCATION_ELEMENT);
      signatureLocation.setText(location);
    }

    Element keyInfo = dsigElement(Constants.KEY_INFO_ELEMENT);
    Element x509Data = dsigElement(Constants.X509_DATA_ELEMENT);
    x509Data.addContent(certificateElement(signingCert));
    for (X509Certificate cert : certificateChain) {
      x509Data.addContent(certificateElement(cert));
    }

    sig.addContent(signedInfo);
    signedInfo.addContent(c14n);
    signedInfo.addContent(signatureMethod);

    if (signatureLocation != null) {
      sig.addContent(signatureLocation);
    }
    sig.addContent(keyInfo);
    keyInfo.addContent(x509Data);
    return sig;
  }

  private Element certificateElement(X509Certificate cert) throws GeneralSecurityException {
    Element e = dsigElement(Constants.X509_CERTIFICATE);
    e.setText(getCertAsString(cert));
    return e;
  }

  private String getCertAsString(X509Certificate cert) throws GeneralSecurityException {
    byte[] der = cert.getEncoded();
    return EncodingUtil.encodeBase64(der);
  }

  private String signDoc(byte[] docBytes) throws GeneralSecurityException {
    // TODO(beaton) support DSA sigs as well?
    Signature signer = Signature.getInstance(Constants.RSA_SHA1_JCE_ID);
    signer.initSign(privateKey);
    signer.update(docBytes);
    return EncodingUtil.encodeBase64(signer.sign());
  }

  private Element dsigElement(String name) {
    return new Element(name, Constants.XML_DSIG_NS);
  }

  private Element simpleSigElement(String name) {
    return new Element(name, Constants.SIMPLE_SIGN_NS);
  }
}
