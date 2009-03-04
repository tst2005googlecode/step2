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

import org.openid4java.discovery.Identifier;

/**
 * A new type of identifier, indicating the identity of a site (IdP) as opposed
 * to that of a user. The identifier will typically not be a URL, but simply
 * the name of the host (e.g. "example.com", as opposed to "http://example.com")
 */
public class IdpIdentifier implements Identifier {

  private final String idp;

  public IdpIdentifier(String idp) {
    this.idp = idp;
  }

  public String getIdentifier() {
    return idp;
  }
}
