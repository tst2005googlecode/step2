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

import com.google.common.collect.Lists;
import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;
import com.google.step2.util.EncodingUtil;
import com.google.step2.util.XmlUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies signatures on XML documents.
 */
public class Verifier {

  private CertValidator validator;
  private byte[] document;
  private HttpFetcher fetcher;

  private String signatureHref;
  private List<X509Certificate> docCerts;

  public Verifier setCertValidator(CertValidator validator) {
    this.validator = validator;
    return this;
  }

  public Verifier setDocument(byte[] document) {
    this.document = document;
    return this;
  }

  public Verifier setFetcher(HttpFetcher fetcher) {
    this.fetcher = fetcher;
    return this;
  }

  public VerificationResult verify() throws XmlSimpleSignException {
    try {
      /* xml parsing bits */
      Document xml = XmlUtil.getJdomDocument(new ByteArrayInputStream(document));
      Element signature = findDsig(xml.getRootElement(), Constants.SIGNATURE_ELEMENT);
      parseSignatureInfo(signature);
      parseSignatureValue(signature);
      parseCerts(signature);
      FetchRequest request = FetchRequest.createGetRequest(URI.create(signatureHref));
      FetchResponse r = fetcher.fetch(request);
      byte[] sig = EncodingUtil.decodeBase64(r.getContentAsBytes());
      return checkSignature(sig);
    } catch (JDOMException e) {
      throw new XmlSimpleSignException("XML error", e);
    } catch (FetchException e) {
      throw new XmlSimpleSignException("couldn't fetch " + signatureHref, e);
    } catch (IOException e) {
      throw new XmlSimpleSignException("XML error", e);
    } catch (GeneralSecurityException e) {
      throw new XmlSimpleSignException("Signature verification error", e);
    } catch (CertValidatorException e) {
      throw new XmlSimpleSignException("Untrusted certificate", e);
    }
  }

  private void parseSignatureInfo(Element signature) throws XmlSimpleSignException {
    Element signedInfo = findDsig(signature, Constants.SIGNED_INFO_ELEMENT);
    if (signedInfo == null) {
      throw new XmlSimpleSignException("No SignedInfo element");
    }
    Element c14n = findDsig(signedInfo, Constants.CANONICALIZATION_METHOD_ELEMENT);
    if (c14n == null) {
      throw new XmlSimpleSignException("No CanonicalizationMethod element");
    }
    String c14nAlg = c14n.getAttributeValue(Constants.ALGORITHM_ATTRIBUTE);
    if (!Constants.CANONICALIZE_RAW_OCTETS.equals(c14nAlg)) {
      throw new XmlSimpleSignException("Unknown canonicalization algorithm: " + c14nAlg);
    }
    Element sigMethod = findDsig(signedInfo, Constants.SIGNATURE_METHOD_ELEMENT);
    if (sigMethod == null) {
      throw new XmlSimpleSignException("No SignatureMethod element");
    }
    // TODO: add support for alternate signing algorithms
    String signingAlg = sigMethod.getAttributeValue(Constants.ALGORITHM_ATTRIBUTE);
    if (!Constants.RSA_SHA1_ALGORITHM.equals(signingAlg)) {
      throw new XmlSimpleSignException("Unknown signing algorithm: " + signingAlg);
    }
  }

  private void parseSignatureValue(Element signature) throws XmlSimpleSignException {
    Element signatureLocation = findSimpleSig(signature, Constants.SIGNATURE_LOCATION_ELEMENT);
    if (signatureLocation == null) {
      throw new XmlSimpleSignException("No SignatureLocation element found");
    }
    signatureHref = signatureLocation.getTextTrim();
    if (signatureHref == null) {
      throw new XmlSimpleSignException("No SignatureLocation text found");
    }
  }

  private void parseCerts(Element signature)
      throws XmlSimpleSignException, GeneralSecurityException {
    Element keyInfo = findDsig(signature, Constants.KEY_INFO_ELEMENT);
    if (keyInfo == null) {
      throw new XmlSimpleSignException("No KeyInfo element found");
    }
    Element x509Data = findDsig(keyInfo, Constants.X509_DATA_ELEMENT);
    if (x509Data == null) {
      throw new XmlSimpleSignException("No X509Data element found");
    }
    List<Element> certs = findElements(x509Data, Constants.X509_CERTIFICATE);
    if (certs.isEmpty()) {
      throw new XmlSimpleSignException("No X509Certificate elements found");
    }
    docCerts = Lists.newArrayList();
    for (Element i : certs) {
      docCerts.add(CertUtil.getCertFromBase64Bytes(i.getTextNormalize()));
    }
  }

  private VerificationResult checkSignature(byte[] sig)
      throws GeneralSecurityException, XmlSimpleSignException, CertValidatorException {
    Signature verifier = Signature.getInstance(Constants.RSA_SHA1_JCE_ID);
    verifier.initVerify(docCerts.get(0).getPublicKey());
    verifier.update(document);
    boolean match = verifier.verify(sig);
    if (!match) {
      throw new XmlSimpleSignException("Signature is invalid");
    }
    validator.verify(docCerts);
    return new VerificationResult(docCerts);
  }

  private List<Element> findElements(Element parent, String name) {
    List<Element> els = new ArrayList<Element>();
    for (Element i : getChildren(parent)) {
      if (name.equals(i.getName()) && Constants.XML_DSIG_NS.equals(i.getNamespace())) {
        els.add(i);
      }
    }
    return els;
  }

  private Element findDsig(Element parent, String name) {
    return find(parent, name, Constants.XML_DSIG_NS);
  }

  private Element findSimpleSig(Element parent, String name) {
    return find(parent, name, Constants.SIMPLE_SIGN_NS);
  }

  private Element find(Element parent, String name, Namespace ns) {
    for (Element i : getChildren(parent)) {
      if (name.equals(i.getName()) && ns.equals(i.getNamespace())) {
        return i;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<Element> getChildren(Element xml) {
    return xml.getChildren();
  }
}
