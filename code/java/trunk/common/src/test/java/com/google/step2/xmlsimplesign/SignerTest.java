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

import com.google.common.collect.ImmutableSet;
import com.google.step2.util.EncodingUtil;

import junit.framework.TestCase;


/**
 * @author brian
 *
 */
public class SignerTest extends TestCase {

  public static final String TEST_DOC = "<foo><bar>Hello World</bar></foo>";

  private CachedCertPathValidator validator =
      new CachedCertPathValidator(ImmutableSet.of(CertConstantUtil.CA_PUB_CERT));

  public void testSign() throws Exception {
    Signer s = new Signer()
        .setSignatureFilePrefix("prefix")
        .setSigningKey(CertConstantUtil.SERVER_PUB_CERT, CertConstantUtil.SERVER_PRIV_KEY)
        .addIntermediateCert(CertConstantUtil.INTERMEDIATE_PUB_CERT)
        .setDocument(EncodingUtil.getUtf8Bytes(TEST_DOC));

    SignatureResult r = s.sign();

    System.out.println(EncodingUtil.getUtf8String(r.getDocument()));

    assertTrue("signature location didn't start with prefix: " + r.getSignatureLocation(),
        r.getSignatureLocation().startsWith("prefix"));

    Verifier v = new Verifier(validator, new FakeFetcher(r.getSignatureLocation(), r.getSignature()));

    VerificationResult vr = v.verify(r.getDocument(), null);
    assertEquals(CertConstantUtil.SERVER_PUB_CERT, vr.getCerts().get(0));
  }

  public void testSign_outOfBandSignature() throws Exception {
    Signer s = new Signer()
        .setSigningKey(CertConstantUtil.SERVER_PUB_CERT, CertConstantUtil.SERVER_PRIV_KEY)
        .addIntermediateCert(CertConstantUtil.INTERMEDIATE_PUB_CERT)
        .setDocument(EncodingUtil.getUtf8Bytes(TEST_DOC));

    SignatureResult r = s.sign();

    System.out.println(EncodingUtil.getUtf8String(r.getDocument()));

    assertNull("there should be no signatureLocation when signing without a prefic",
        r.getSignatureLocation());

    Verifier v = new Verifier(validator, new FakeFetcher(r.getSignatureLocation(), r.getSignature()));

    VerificationResult vr = v.verify(r.getDocument(), r.getSignature());
    assertEquals(CertConstantUtil.SERVER_PUB_CERT, vr.getCerts().get(0));
  }

  public void testSignNoIntermediate() throws Exception {
    Signer s = new Signer()
        .setSignatureFilePrefix("prefix")
        .setSigningKey(CertConstantUtil.SERVER_PUB_CERT, CertConstantUtil.SERVER_PRIV_KEY)
        .setDocument(EncodingUtil.getUtf8Bytes(TEST_DOC));

    SignatureResult r = s.sign();
    assertTrue("signature location didn't start with prefix: " + r.getSignatureLocation(),
        r.getSignatureLocation().startsWith("prefix"));

    CachedCertPathValidator validator = new CachedCertPathValidator(
        ImmutableSet.of(CertConstantUtil.INTERMEDIATE_PUB_CERT));

    Verifier v = new Verifier(validator,
        new FakeFetcher(r.getSignatureLocation(), r.getSignature()));

    VerificationResult vr = v.verify(r.getDocument(), null);
    assertEquals(CertConstantUtil.SERVER_PUB_CERT, vr.getCerts().get(0));
  }

  public void testBadSignature() throws Exception {
    Signer s = new Signer()
        .setSignatureFilePrefix("prefix")
        .setSigningKey(CertConstantUtil.SERVER_PUB_CERT, CertConstantUtil.SERVER_PRIV_KEY)
        .addIntermediateCert(CertConstantUtil.INTERMEDIATE_PUB_CERT)
        .setDocument(EncodingUtil.getUtf8Bytes(TEST_DOC));

    SignatureResult r = s.sign();
    assertTrue("signature location didn't start with prefix: " + r.getSignatureLocation(),
        r.getSignatureLocation().startsWith("prefix"));

    String origDoc = EncodingUtil.getUtf8String(r.getDocument());
    String tampered = origDoc.replaceAll("foo", "bar");
    Verifier v = new Verifier(validator, new FakeFetcher(r.getSignatureLocation(), r.getSignature()));
    try {
      v.verify(EncodingUtil.getUtf8Bytes(tampered), null);
      fail("Signature verification should have failed");
    } catch (XmlSimpleSignException e) {
      // good.
    }
  }
}
