/**
 * Copyright 2010 Google Inc.
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
package com.google.step2.discovery;

import junit.framework.TestCase;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class SecureDiscoveryInformationTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testCreation() throws Exception {
    URL url = new URL("http://whatever.com");
    UrlIdentifier identifier = new UrlIdentifier("http://identifier.com");
    Set types = Collections.singleton("Type");
    DiscoveryInformation information = new DiscoveryInformation(
        url, identifier, "delegate", "version", types);

    SecureDiscoveryInformation secureInformation =
        new SecureDiscoveryInformation(information);

    assertEquals(url, secureInformation.getOPEndpoint());
    assertEquals(identifier, secureInformation.getClaimedIdentifier());
    assertEquals("delegate", secureInformation.getDelegateIdentifier());
    assertEquals("version", secureInformation.getVersion());
    assertEquals(types, secureInformation.getTypes());
  }
}
