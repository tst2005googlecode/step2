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

/**
 * Thrown when something goes wrong while fetching data over HTTP
 */
public class FetchException extends Exception {

  public FetchException() {
    super();
  }

  public FetchException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param request the FetchRequest we couldn't successfully complete.
   */
  public FetchException(FetchRequest request, Throwable cause) {
    this("Couldn't fetch " + request.getUri().toASCIIString(), cause);
  }

  public FetchException(String message) {
    super(message);
  }

  public FetchException(Throwable cause) {
    super(cause);
  }
}
