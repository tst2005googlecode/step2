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

import java.io.InputStream;

/**
 * Interface representing the response from an HTTP request. Implementations
 * that provide a custom implementation of HttpFetcher also need to provide
 * an implementation of this interface.
 */
public interface FetchResponse {

  /**
   * Returns the status code of this HTTP response.
   */
  public int getStatusCode();

  /**
   * Returns the contents of this HTTP response, as an InputStream
   */
  public InputStream getContentAsStream() throws FetchException;

  /**
   * Returns the contents of this HTTP response, as a byte array
   */
  public byte[] getContentAsBytes() throws FetchException;

  /**
   * Returns the value of the first header with the given name, null if
   * no such header was found.
   */
  public String getFirstHeader(String name);
}
