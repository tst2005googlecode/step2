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

import junit.framework.TestCase;

public class DefaultCertValidatorTest extends TestCase {

  private DefaultCertValidator validator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    validator = new DefaultCertValidator();
  }

  public void testMatch() throws Exception {

    assertTrue(validator.matches(CertConstantUtil.SERVER_PUB_CERT,
        "Server Cert"));

    assertFalse(validator.matches(CertConstantUtil.SERVER_PUB_CERT,
        "foobar"));
  }

  public void testGetCnFromDn() throws Exception {
    assertEquals("Server Cert", validator.getCnFromDn("CN=Server Cert"));
    assertEquals("hosted-id.google.com",
        validator.getCnFromDn("C=US, ST=California, O=Google Inc, CN=hosted-id.google.com"));
    assertEquals("hosted-id.google.com",
        validator.getCnFromDn("CN=hosted-id.google.com, C=US, ST=California, O=Google Inc"));
    assertNull(
        validator.getCnFromDn("OU=hosted-id.google.com, C=US, ST=California, O=Google Inc"));
    assertEquals("Server Cert",
        validator.getCnFromDn("C=US, ST=California, O=Google Inc, CN=Server Cert"));
    assertEquals("Server Cert",
        validator.getCnFromDn("CN=Server Cert, C=US, ST=California, O=Google Inc"));
  }
}
