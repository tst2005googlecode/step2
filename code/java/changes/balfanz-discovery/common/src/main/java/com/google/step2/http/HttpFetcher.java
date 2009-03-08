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
package com.google.step2.http;

import com.google.inject.ImplementedBy;

/**
 * Simple interface for fetching data over HTTP. This is here simply so that
 * implementation can easily replace the default (Apache HTTP-client-based)
 * implementation.
 */
@ImplementedBy(DefaultHttpFetcher.class)
public interface HttpFetcher {

  /**
   * Fetch some data over HTTP. Follow redirects during the fetch.
   *
   * @throws FetchException if there is an error during the run of the
   *   HTTP protocol itself. This does not include HTTP error responses (which
   *   are returned in the FetchResponse).
   */
  FetchResponse fetch(FetchRequest request) throws FetchException;
}
