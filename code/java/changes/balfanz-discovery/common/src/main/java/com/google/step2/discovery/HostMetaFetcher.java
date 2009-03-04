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

import com.google.inject.ImplementedBy;

/**
 * Interface for classes that implement various host-meta fetching
 * strategies. The default implementation will just attempto to fetch the
 * host-meta from the /host-meta path of a site.
 */
@ImplementedBy(DefaultHostMetaFetcher.class)
public interface HostMetaFetcher {

  /**
   * Returns the host-meta for a given site.
   * @param host the name of the host, including port (e.g. "foo.com", or
   *   "bar.com:9080", or "www.foo.com").
   * @return the host-meta
   * @throws HostMetaException if the host-meta cannot be fetched.
   */
  public HostMeta getHostMeta(String host) throws HostMetaException;

}
