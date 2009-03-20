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

import static org.easymock.classextension.EasyMock.expect;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.security.cert.X509Certificate;

public class DisjunctiveCertValidatorTest extends TestCase {

  private IMocksControl control;
  private CertValidator val1;
  private CertValidator val2;
  private DisjunctiveCertValidator validator;
  private String authority;
  private X509Certificate cert;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    authority = "authority";
    cert = CertConstantUtil.SERVER_PUB_CERT;
    control = EasyMock.createControl();
    val1 = control.createMock(CertValidator.class);
    val2 = control.createMock(CertValidator.class);

    validator = new DisjunctiveCertValidator(val1, val2);
  }

  public void testOneSucceeds() throws Exception {
    expect(val1.matches(cert, authority)).andStubReturn(Boolean.TRUE);
    expect(val2.matches(cert, authority)).andStubReturn(Boolean.FALSE);
    control.replay();
    assertTrue(validator.matches(cert, authority));
    control.verify();
  }

  public void testOtherSucceeds() throws Exception {
    expect(val1.matches(cert, authority)).andStubReturn(Boolean.FALSE);
    expect(val2.matches(cert, authority)).andStubReturn(Boolean.TRUE);
    control.replay();
    assertTrue(validator.matches(cert, authority));
    control.verify();
  }

  public void testBothSucceed() throws Exception {
    expect(val1.matches(cert, authority)).andStubReturn(Boolean.TRUE);
    expect(val2.matches(cert, authority)).andStubReturn(Boolean.TRUE);
    control.replay();
    assertTrue(validator.matches(cert, authority));
    control.verify();
  }

  public void testNeitherSucceeds() throws Exception {
    expect(val1.matches(cert, authority)).andStubReturn(Boolean.FALSE);
    expect(val2.matches(cert, authority)).andStubReturn(Boolean.FALSE);
    control.replay();
    assertFalse(validator.matches(cert, authority));
    control.verify();
  }
}
