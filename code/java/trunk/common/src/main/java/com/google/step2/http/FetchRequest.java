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

import java.net.URI;

/**
 * Class that represents an HTTP request.
 */
public class FetchRequest {

  public enum Method {
    GET,
    HEAD,
    POST
  }

  private final Method method;
  private final URI uri;

  public static FetchRequest createGetRequest(URI uri) {
    return new FetchRequest(Method.GET, uri);
  }

  public static FetchRequest createHeadRequest(URI uri) {
    return new FetchRequest(Method.HEAD, uri);
  }

  public static FetchRequest createPostRequest(URI uri) {
    return new FetchRequest(Method.POST, uri);
  }

  public FetchRequest(Method method, URI uri) {
    this.method = method;
    this.uri = uri;
  }

  /**
   * Returns the method (GET, POST, etc.) of this HTTP request
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Returns the URI of this HTTP request.
   */
  public URI getUri() {
    return uri;
  }

  // implementing hashCode and equals so we can use these in EasyMock-based
  // test cases like this:
  //
  // ...
  // // the request going out should look like this:
  // FetchRequest expectedRequest = ...;
  //
  // expect(httpFetcher.fetch(expectedRequest));
  // ...
  //
  // which will use .equals() to compare the actual and expected fetch requests.
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    FetchRequest other = (FetchRequest) obj;
    if (method == null) {
      if (other.method != null) return false;
    } else if (!method.equals(other.method)) return false;
    if (uri == null) {
      if (other.uri != null) return false;
    } else if (!uri.equals(other.uri)) return false;
    return true;
  }
}
