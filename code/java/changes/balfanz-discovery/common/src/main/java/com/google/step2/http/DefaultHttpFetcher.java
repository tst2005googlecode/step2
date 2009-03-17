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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;

import java.io.IOException;
import java.io.InputStream;

/**
 * Default implementations of HttpFetcher and FetchResponse. Based on
 * Apache http client. We use the {@link ProxySelectorRoutePlanner}, which means
 * that we pick up the default proxy set in the VM (can be set through
 * {@link java.net.ProxySelector}.setDefault(), or by specifying system
 * properties http.proxyHost, http.proxyPort, etc.).
 */
public class DefaultHttpFetcher implements HttpFetcher {

  private final DefaultHttpClient httpClient;

  public DefaultHttpFetcher() {
    // this follows redirects by default
    this.httpClient = new DefaultHttpClient();

    // this means you can set a proxy through
    // java.net.ProxySelector.setDefault(), or by simply starting the
    // jvm with -Dhttp.proxyHost=foo.com -Dhttp.proxyPort=8080
    httpClient.setRoutePlanner(new ProxySelectorRoutePlanner(
        httpClient.getConnectionManager().getSchemeRegistry(),
        null));
  }

  public FetchResponse fetch(FetchRequest request) throws FetchException {

    HttpUriRequest uriRequest;

    switch (request.getMethod()) {
      case GET:
        uriRequest = new HttpGet(request.getUri());
        break;
      case POST:
        uriRequest = new HttpPost(request.getUri());
        break;
      case HEAD:
        uriRequest = new HttpHead(request.getUri());
        break;
      default:
        throw new FetchException("unsupported HTTP method: " +
            request.getMethod());
    }

    try {
      return new DefaultFetchResponse(httpClient.execute(uriRequest));
    } catch (ClientProtocolException e) {
      throw new FetchException(request, e);
    } catch (IOException e) {
      throw new FetchException(request, e);
    }
  }

  private static class DefaultFetchResponse implements FetchResponse {

    private final HttpResponse response;

    public DefaultFetchResponse(HttpResponse response) {
      this.response = response;
    }

    public int getStatusCode() {
      return response.getStatusLine().getStatusCode();
    }

    public InputStream getContentAsStream() throws FetchException {
      try {
        return response.getEntity().getContent();
      } catch (IllegalStateException e) {
        throw new FetchException(e);
      } catch (IOException e) {
        throw new FetchException(e);
      }
    }

    public byte[] getContentAsBytes() throws FetchException {
      try {
        return IOUtils.toByteArray(getContentAsStream());
      } catch (IOException e) {
        throw new FetchException(e);
      }
    }

    public String getFirstHeader(String name) {
      Header header = response.getFirstHeader(name);
      if (header == null) {
        return null;
      }
      return header.getValue();
    }
  }
}
