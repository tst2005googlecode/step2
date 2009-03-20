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
package com.google.step2.util;

import com.google.step2.util.ExpiringLruCache;
import com.google.step2.util.FakeTimeSource;

import junit.framework.TestCase;

/**
 *
 */
public class ExpiringLruCacheTest extends TestCase {

  private static final int MAX = 5;

  private ExpiringLruCache<Integer, Integer> cache;
  private FakeTimeSource timeSource = new FakeTimeSource();

  @Override
  public void setUp() {
    cache = new ExpiringLruCache<Integer, Integer>(MAX);
    cache.setTimeSource(timeSource);
  }

  public void testMaxSize() {
    for (int i = 0; i < MAX; ++i) {
      cache.put(i, i, 1000);
    }
    for (int i = 0; i < MAX; ++i) {
      assertEquals(new Integer(i), cache.get(i));
    }
    cache.put(6, 6, 1000);
    assertNull(cache.get(0));
  }

  public void testExpiration() {
    for (int i = 0; i < MAX; ++i) {
      cache.put(i, i, 1000);
    }
    for (int i = 0; i < MAX; ++i) {
      assertEquals(new Integer(i), cache.get(i));
    }
    timeSource.advanceSeconds(1001);
    for (int i = 0; i < MAX; ++i) {
      assertNull(cache.get(i));
    }
  }
}
