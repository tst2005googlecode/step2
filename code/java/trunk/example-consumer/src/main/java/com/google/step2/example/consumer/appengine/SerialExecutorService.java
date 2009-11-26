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
package com.google.step2.example.consumer.appengine;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes Callables in the current thread.
 */
public class SerialExecutorService implements ExecutorService {

  public boolean awaitTermination(long timeout, TimeUnit unit) {
    return false;
  }

  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {

    List<Future<T>> futures = Lists.newArrayListWithCapacity(tasks.size());
    for (Callable<T> task : tasks) {
      futures.add(submit(task));
    }
    return futures;
  }

  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
      long timeout, TimeUnit unit) {
    return invokeAll(tasks);
  }

  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws ExecutionException {

    Exception lastException = null;
    for (Callable<T> task : tasks) {
      try {
        T result = task.call();
        return result;
      } catch (Exception e) {
        lastException = e;
        continue;
      }
    }

    throw new ExecutionException("none of the tasks completed successfully",
        lastException);
  }

  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) throws ExecutionException {
    return invokeAny(tasks);
  }

  public boolean isShutdown() {
    return false;
  }

  public boolean isTerminated() {
    return false;
  }

  public void shutdown() {
  }

  public List<Runnable> shutdownNow() {
    return ImmutableList.of();
  }

  public <T> Future<T> submit(Callable<T> task) {
    try {
      T result = task.call();
      return new NoFuture<T>(result);
    } catch (Exception e) {
      return new NoFuture<T>(new ExecutionException(e));
    }
  }

  public Future<?> submit(Runnable task) {
    return submit(Executors.callable(task));
  }

  public <T> Future<T> submit(Runnable task, T result) {
    task.run();
    return new NoFuture<T>(result);
  }

  public void execute(Runnable command) {
    command.run();
  }

  private class NoFuture<T> implements Future<T> {

    private final T value;
    private final ExecutionException ex;

    public NoFuture(T value) {
      this.value = value;
      this.ex = null;
    }

    public NoFuture(ExecutionException ex) {
      this.value = null;
      this.ex = ex;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    public T get() throws ExecutionException {
      if (ex == null) {
        return value;
      } else {
        throw ex;
      }
    }

    public T get(long timeout, TimeUnit unit) throws ExecutionException {
      return get();
    }

    public boolean isCancelled() {
      return false;
    }

    public boolean isDone() {
      return true;
    }
  }
}
