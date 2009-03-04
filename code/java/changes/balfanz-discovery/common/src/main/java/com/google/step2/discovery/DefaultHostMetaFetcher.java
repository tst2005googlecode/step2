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

import com.google.inject.Inject;
import com.google.step2.http.HttpFetcher;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implements the default strategy to fetch host-meta files: from the
 * /host-meta path of the site in question.
 */
public class DefaultHostMetaFetcher extends UrlHostMetaFetcher {

  private final static String HOST_META_PATH = "/host-meta";

  @Inject
  public DefaultHostMetaFetcher(HttpFetcher fetcher) {
    super(fetcher);
  }

  @Override
  protected URI getHostMetaUriForHost(String host) throws URISyntaxException {
    return new URI("http", host, HOST_META_PATH, null);
  }
}
