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
import java.util.concurrent.CountDownLatch;

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

  /**
   * Public constructor.
   * @param fetchers the fetchers that we will try in parallel.
   */
  public ParallelHostMetaFetcher(HostMetaFetcher... fetchers) {
    this.fetchers = Arrays.asList(fetchers);
  }

  public HostMeta getHostMeta(String host) throws HostMetaException {
    ParallelFetchRequest request = new ParallelFetchRequest(host);
    return request.get();
  }

  /**
   * Encapsulates the state we need to keep track of multiple host-meta
   * fetches.
   */
  private class ParallelFetchRequest {

    private final CountDownLatch latch;
    private final List<FetcherThread> fetcherThreads;
    private HostMeta result;
    private HostMetaException exception;

    public ParallelFetchRequest(String host) {
      latch = new CountDownLatch(fetchers.size());
      fetcherThreads = new ArrayList<FetcherThread>();
      for (HostMetaFetcher fetcher : fetchers) {
        FetcherThread thread = new FetcherThread(fetcher, host);
        fetcherThreads.add(thread);
        thread.start();
      }
    }

    /**
     * Cancels all fetcher threads, except for the one provided (presumably,
     * because we already know that that thread has finished).
     *
     * @param fetcherThread the thread that we don't need to cancel.
     */
    public void cancelAllFetchersExcept(FetcherThread fetcherThread) {
      for (FetcherThread thread : fetcherThreads) {
        if (thread != fetcherThread) {
          thread.cancel();
        }
      }
    }

    public HostMeta get() throws HostMetaException {
      do {
        try {
          latch.await();
        } catch (InterruptedException e) {
          // we got interrupted. just loop around and wait some more.
        }
      } while (latch.getCount() > 0);

      if (result != null) {
        return result;
      }

      if (exception != null) {
        throw exception;
      }

      throw new HostMetaException("none of the fetchers returned.");
    }

    /**
     * Thread in which we execute one particular fetch.
     */
    private class FetcherThread extends Thread {

      private final String host;
      private final HostMetaFetcher fetcher;

      public FetcherThread(HostMetaFetcher fetcher, String host) {
        this.fetcher = fetcher;
        this.host = host;
      }

      public void cancel() {
        // if the HostMetaFetcher interface was richer, we would actually
        // try and cancel the fetching of the host-meta here.
        // For now, we'll just signal this thread as done.
        latch.countDown();
      }

      @Override
      public void run() {
        try {
          result = fetcher.getHostMeta(host);
          cancelAllFetchersExcept(this);
        } catch(HostMetaException e) {
          exception = e;
        } finally {
          latch.countDown();
        }
      }
    }
  }
}
