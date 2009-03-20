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

import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Super-class for implementations of HostMetaFetcher that fetch a host-meta
 * file from a certain URL. A subclass of this class can merely indicate which
 * URL the host-meta should be fetched from, and this class will then take
 * care of the actual fetching of the bits, and the parsing of the file.
 */
public abstract class UrlHostMetaFetcher implements HostMetaFetcher {

  private final HttpFetcher fetcher;

  /**
   * Constructor. An UrlHostMetaFetcher is a HostMetaFetcher that needs
   * an HttpFetcher to do its job.
   */
  protected UrlHostMetaFetcher(HttpFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public HostMeta getHostMeta(String host) throws HostMetaException {
    try {
      URI uri = getHostMetaUriForHost(host);
      FetchRequest request = FetchRequest.createGetRequest(uri);

      FetchResponse response = fetcher.fetch(request);

      int status = response.getStatusCode();

      if (status != HttpStatus.SC_OK) {
        throw new HttpResponseException(status, "fetching host-meta from " +
            host + " return status " + status);
      }

      return HostMeta.parseFromStream(response.getContentAsStream());

    } catch (FetchException e) {
      throw new HostMetaException(e);
    } catch (URISyntaxException e) {
      throw new HostMetaException(e);
    } catch (HttpResponseException e) {
      throw new HostMetaException(e);
    } catch (IOException e) {
      throw new HostMetaException(e);
    }
  }

  /**
   * Given a host, returns the URL from which to download the host-meta for
   * that host.
   */
  protected abstract URI getHostMetaUriForHost(String host)
      throws URISyntaxException;
}
