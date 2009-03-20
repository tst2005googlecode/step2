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


import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Cache supporting both LRU and time-based expiry.
 *
 * LRU: once maximum size is reached, the least recently accessed element is discarded.
 *
 * Time-based: entries are discarded once they reach a maximum age.
 */
public class ExpiringLruCache<K, V> {

  private final LruLinkedHashMap<K, V> map;

  private TimeSource timeSource = new TimeSource();

  public ExpiringLruCache(int capacity) {
    map = new LruLinkedHashMap<K, V>(capacity);
  }

  public void setTimeSource(TimeSource timeSource) {
    this.timeSource = timeSource;
  }

  public void put(K key, V value, long maxSeconds) {
    synchronized(map) {
      long maxAge = timeSource.currentTimeMillis() + maxSeconds * 1000L;
      map.put(key, new EntryWithAge<V>(value, maxAge));
    }
  }

  public V get(K key) {
    synchronized(map) {
      EntryWithAge<V> entry = map.get(key);
      if (entry != null && timeSource.currentTimeMillis() < entry.expireMillis) {
        return entry.value;
      }
      return null;
    }
  }

  private static class EntryWithAge<V> {
    private final V value;
    private final long expireMillis;

    public EntryWithAge(V value, long expireMillis) {
      this.value = value;
      this.expireMillis = expireMillis;
    }
  }

  private static class LruLinkedHashMap<K, V> extends LinkedHashMap<K, EntryWithAge<V>> {

    private final int capacity;

    public LruLinkedHashMap(int capacity) {
      super(capacity, 0.75f, true);
      this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, EntryWithAge<V>> eldest) {
      return this.size() > capacity;
    }
  }
}
