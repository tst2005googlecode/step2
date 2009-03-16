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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Uses multiple fetchers in parallel to obtain a host-meta for a given site.
 * For example, one of the fetchers can look for the host-meta in its standard
 * location (http://host/host-meta), while other fetchers try other strategies
 * (getting host-metas from a database, or fetching them from a hosting service
 * to which the host may have outsourced the hosting of host-metas).
 *
 * If more than one fetchers succeed, it is undefined which result this fetcher
 * returns. Likewise, if none of them succeed, it is undefined which fetcher's
 * exception is propagated to the caller. We _do_ guarantee that if at least one
 * fetcher succeeds, the parallel fetcher will also succeed.
 */
public class ParallelHostMetaFetcher implements HostMetaFetcher {

  private final List<HostMetaFetcher> fetchers;
  private final ExecutorService executorService;
  private final long timeout; // in seconds

  /**
   * Public constructor.
   * @param executorService the ExecutorService that will run the various
   *   threads in which we'll attempt the parallel fetching.
   * @param timeout timeout, in seconds, of how long we're willing to wait for
   *   the parallel host-meta fetchers to fetch a host-meta
   * @param fetchers the fetchers that we will try in parallel.
   */
  public ParallelHostMetaFetcher(ExecutorService executorService,
      Long timeout, HostMetaFetcher... fetchers) {
    if (fetchers.length == 0) {
      throw new IllegalArgumentException("need to supply at least one " +
          "HostMetaFetcher to ParallelHostMetaFetcher");
    }
    this.fetchers = Arrays.asList(fetchers);
    this.executorService = executorService;
    this.timeout = timeout.longValue();
  }

  public HostMeta getHostMeta(String host) throws HostMetaException {
    List<Callable<HostMeta>> threads =
        new ArrayList<Callable<HostMeta>>(fetchers.size());
    for (HostMetaFetcher fetcher : fetchers) {
      threads.add(new FetcherThread(fetcher, host));
    }

    try {
      return executorService.invokeAny(threads, timeout, TimeUnit.SECONDS);

    } catch (InterruptedException e) {
      throw new HostMetaException(e);
    } catch (ExecutionException e) {
      throw new HostMetaException("no fetcher found a host-meta for " + host, e);
    } catch (RejectedExecutionException e) {
      throw new HostMetaException("could not schedule threads for parallel " +
          "fetching of host-meta for " + host, e);
    } catch (TimeoutException e) {
      throw new HostMetaException("none of the host-meta fetchers completed " +
          "within " + timeout + " seconds for host " + host, e);
    }
  }

  /**
   * Thread in which we execute one particular fetch.
   */
  private class FetcherThread implements Callable<HostMeta> {

    private final String host;
    private final HostMetaFetcher fetcher;

    public FetcherThread(HostMetaFetcher fetcher, String host) {
      this.fetcher = fetcher;
      this.host = host;
    }

    public HostMeta call() throws HostMetaException {
      HostMeta hostMeta = fetcher.getHostMeta(host);
      if ((hostMeta == null)
          || (0 == (hostMeta.getLinks().size() + hostMeta.getLinkPatterns().size()))) {
        throw new HostMetaException("fetcher " +
            fetcher.getClass().getName() +
            " returned empty host-meta for " + host);
      } else {
        return hostMeta;
      }
    }
  }
}
