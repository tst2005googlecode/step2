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
package com.google.step2.discovery;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;

/**
 * A {@link UrlIdentifier} that signifies that discovery on this identifier
 * was performed securely. The getUrl method returns the same URL as a normal
 * {@link UrlIdentifier} would, but the getIdentifier method returns a different
 * String, ensuring that a securely discovered identifier and an insecurely
 * discovered identifier with the same URL don't get treated as the same user
 * (assuming that an RP would use the String returned by getIdentifier() to
 * identify users in their system).
 */
public class SecureUrlIdentifier extends UrlIdentifier {

  public SecureUrlIdentifier(Identifier id) throws DiscoveryException {
    super(id.getIdentifier());
  }

  @Override
  public String getIdentifier() {
    return "secure:" + super.getIdentifier();
  }
}
