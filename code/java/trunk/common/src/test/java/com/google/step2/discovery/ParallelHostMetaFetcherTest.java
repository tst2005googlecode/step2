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

import static org.easymock.classextension.EasyMock.expect;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import java.util.concurrent.Executors;

public class ParallelHostMetaFetcherTest extends TestCase {

  private IMocksControl control;
  private HostMetaFetcher fetcher1;
  private HostMetaFetcher fetcher2;
  private HostMeta hostMeta1;
  private HostMeta hostMeta2;
  private ParallelHostMetaFetcher fetcher;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    fetcher1 = control.createMock(HostMetaFetcher.class);
    fetcher2 = control.createMock(HostMetaFetcher.class);
    hostMeta1 = new HostMeta();
    hostMeta1.addLink(Link.fromString("Link: <http://foo.com>"));
    hostMeta2 = new HostMeta();
    hostMeta2.addLink(Link.fromString("Link: <http://foo.com>"));
    fetcher = new ParallelHostMetaFetcher(
        Executors.newFixedThreadPool(2), 10L, fetcher1, fetcher2);
  }

  public void testGet_bothSucceed() throws Exception {
    String host = "host";
    expect(fetcher1.getHostMeta(host)).andStubReturn(hostMeta1);
    expect(fetcher2.getHostMeta(host)).andStubReturn(hostMeta2);
    control.replay();
    HostMeta result = fetcher.getHostMeta(host);
    control.verify();
    assertTrue((result == hostMeta1) || (result == hostMeta2));
  }

  public void testGet_bothThrow() throws Exception {
    String host = "host";
    expect(fetcher1.getHostMeta(host)).andStubThrow(new HostMetaException());
    expect(fetcher2.getHostMeta(host)).andStubThrow(new HostMetaException());
    control.replay();
    try {
      HostMeta result = fetcher.getHostMeta(host);
      fail("expected exception, but didn't get it");
    } catch (HostMetaException e) {
      // expected
    }
    control.verify();
  }

  public void testGet_firstSucceeds() throws Exception {
    String host = "host";
    expect(fetcher1.getHostMeta(host)).andStubReturn(hostMeta1);
    expect(fetcher2.getHostMeta(host)).andStubThrow(new HostMetaException());
    control.replay();
    HostMeta result = fetcher.getHostMeta(host);
    control.verify();
    assertSame(hostMeta1, result);
  }

  public void testGet_secondSucceeds() throws Exception {
    String host = "host";
    expect(fetcher1.getHostMeta(host)).andStubThrow(new HostMetaException());
    expect(fetcher2.getHostMeta(host)).andStubReturn(hostMeta2);
    control.replay();
    HostMeta result = fetcher.getHostMeta(host);
    control.verify();
    assertSame(hostMeta2, result);
  }
}
