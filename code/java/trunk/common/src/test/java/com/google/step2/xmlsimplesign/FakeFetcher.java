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

import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;
import com.google.step2.util.EncodingUtil;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * @author brian
 *
 */
public class FakeFetcher implements HttpFetcher {

  private final String signatureLocation;
  private final String signature;

  public FakeFetcher(String signatureLocation, String signature) {
    this.signatureLocation = signatureLocation;
    this.signature = signature;
  }


  public FetchResponse fetch(FetchRequest request) throws FetchException {
    String url = request.getUri().toString();
    if (url.equals(signatureLocation)) {
      return new FetchResponse() {

        public byte[] getContentAsBytes() {
          return EncodingUtil.getUtf8Bytes(signature);
        }

        public InputStream getContentAsStream() {
          return new ByteArrayInputStream(getContentAsBytes());
        }

        public int getStatusCode() {
          return 200;
        }

        public String getFirstHeader(String name) {
          return null;
        }
      };
    }
    throw new FetchException("Unexpected request for " + url + ", should have been " +
        signatureLocation);
  }
}
