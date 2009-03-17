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

import static org.easymock.EasyMock.expect;

import com.google.step2.http.FetchException;
import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

public class DefaultHostMetaFetcherTest extends TestCase {

  private IMocksControl control;
  private HttpFetcher http;
  private DefaultHostMetaFetcher fetcher;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    http = control.createMock(HttpFetcher.class);
    fetcher = new DefaultHostMetaFetcher(http);
  }

  public void testGet() throws Exception {
    String host = "host.com";
    FetchRequest request =
        FetchRequest.createGetRequest(new URI("http://host.com/host-meta"));
    FetchResponse response = new FakeFetchResponse(200,
        "Link: <http://foo.com/bar>; rel=foobar; type=foo\n");

    expect(http.fetch(request)).andReturn(response);

    control.replay();
    HostMeta hostMeta = fetcher.getHostMeta(host);
    control.verify();

    assertEquals(1, hostMeta.getLinks().size());
  }

  public void testGet_syntaxError() throws Exception {
    String host = "host.com";
    FetchRequest request =
        FetchRequest.createGetRequest(new URI("http://host.com/host-meta"));
    FetchResponse response = new FakeFetchResponse(200,
        "Link: <http://foo.com/bar>; rel=foobar type=foo\n");

    expect(http.fetch(request)).andReturn(response);

    control.replay();
    HostMeta hostMeta = fetcher.getHostMeta(host);
    control.verify();

    assertEquals(0, hostMeta.getLinks().size());
  }

  public void testGet_fetchException() throws Exception {
    String host = "host.com";
    FetchRequest request =
        FetchRequest.createGetRequest(new URI("http://host.com/host-meta"));

    expect(http.fetch(request)).andThrow(new FetchException());

    control.replay();
    try {
      HostMeta hostMeta = fetcher.getHostMeta(host);
      fail("expected exception, but didn't get it");
    } catch (HostMetaException e) {
      // expected
    }
    control.verify();
  }

  private static class FakeFetchResponse implements FetchResponse {

    private final int status;
    private final String response;

    public FakeFetchResponse(int statusCode, String responseContent) {
      this.status = statusCode;
      this.response = responseContent;
    }

    public InputStream getContentAsStream() {
      return new ByteArrayInputStream(response.getBytes());
    }

    public byte[] getContentAsBytes() {
      return response.getBytes();
    }

    public int getStatusCode() {
      return status;
    }

    public String getFirstHeader(String name) {
      return null;
    }
  }
}
