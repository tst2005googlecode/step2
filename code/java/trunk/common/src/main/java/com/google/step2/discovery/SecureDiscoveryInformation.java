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
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;

import java.net.URL;

/**
 * A subclass of {@link DiscoveryInformation} that can keep track of whether
 * discovery information was obtained securely or not. Discovery information
 * is considered to have been obtained "securely" if the chain of XRD documents
 * that led to the discovery information were all properly signed and delegated
 * to the next XRD document in the chain.
 */
public class SecureDiscoveryInformation extends DiscoveryInformation {

  private boolean secure = false;

  public SecureDiscoveryInformation(URL opEndpoint,
      Identifier claimedIdentifier, String delegate, String version)
      throws DiscoveryException {
    super(opEndpoint, claimedIdentifier, delegate, version);
  }

  public SecureDiscoveryInformation(URL opEndpoint) throws DiscoveryException {
    super(opEndpoint);
  }

  public SecureDiscoveryInformation(DiscoveryInformation info)
      throws DiscoveryException {
    this(info.getOPEndpoint(),
         info.getClaimedIdentifier(),
         info.getDelegateIdentifier(),
         info.getVersion());
    if (info instanceof SecureDiscoveryInformation) {
      this.setSecure(((SecureDiscoveryInformation) info).isSecure());
    } else {
      this.setSecure(false);
    }
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  // we need this for the unit tests
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SecureDiscoveryInformation other = (SecureDiscoveryInformation) obj;
    if (secure != other.secure) return false;
    if (!areEqual(this.getClaimedIdentifier(), other.getClaimedIdentifier())) {
      return false;
    }
    if (!areEqual(this.getDelegateIdentifier(), other.getDelegateIdentifier())) {
      return false;
    }
    if (!areEqual(this.getOPEndpoint(), other.getOPEndpoint())) {
      return false;
    }
    if (!areEqual(this.getVersion(), other.getVersion())) {
      return false;
    }
    return true;
  }

  private static <T> boolean areEqual(T o1, T o2) {
    return (o1 == null) ? o2 == null : o1.equals(o2);
  }
}