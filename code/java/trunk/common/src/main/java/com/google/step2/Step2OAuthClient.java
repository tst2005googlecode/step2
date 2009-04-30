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


package com.google.step2;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;

import net.oauth.client.OAuthClient;
import net.oauth.http.HttpClient;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

@Singleton
public class Step2OAuthClient extends OAuthClient {

  @Inject
  public Step2OAuthClient(HttpFetcher httpFetcher) {
    super(convertFetcherToOAuthHttpClient(httpFetcher));
  }

  private static HttpClient convertFetcherToOAuthHttpClient(
      HttpFetcher httpFetcher) {
    return new Step2HttpClient(httpFetcher);
  }

  private static class Step2HttpClient implements HttpClient {

    private final HttpFetcher httpFetcher;

    public Step2HttpClient(HttpFetcher httpFetcher) {
      this.httpFetcher = httpFetcher;
    }

    public HttpResponseMessage execute(HttpMessage request) throws IOException {
      try {
        FetchRequest fetchRequest;
        if ("post".equals(request.method.toLowerCase())) {
          fetchRequest = FetchRequest.createPostRequest(request.url.toURI());
        } else if ("head".equals(request.method.toLowerCase())) {
          fetchRequest = FetchRequest.createHeadRequest(request.url.toURI());
        } else {
          fetchRequest = FetchRequest.createGetRequest(request.url.toURI());
        }
        FetchResponse response = httpFetcher.fetch(fetchRequest);
        return new Step2HttpResponseMessage(response, request);
      } catch (URISyntaxException e) {
        throw new IOException("couldn't convert " + request.url.toString() +
            " into a URI: " + e.getMessage());
      } catch (FetchException e) {
        throw new IOException("couldn't fetch data from " +
            request.url.toString() + ": " + e.getMessage());
      }
    }
  }

  private static class Step2HttpResponseMessage extends HttpResponseMessage {

    private final FetchResponse fetchResponse;

    public Step2HttpResponseMessage(FetchResponse response, HttpMessage request) {
      super(request.method, request.url);
      this.fetchResponse = response;
    }

    @Override
    public int getStatusCode() {
      return fetchResponse.getStatusCode();
    }

    @Override
    protected InputStream openBody() throws IOException {
      try {
        return fetchResponse.getContentAsStream();
      } catch (FetchException e) {
        throw new IOException("could not fetch contents from " + this.url);
      }
    }
  }
}
