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


import java.util.Date;

/**
 *
 */
public class FakeTimeSource extends TimeSource {

  private long now;

  public FakeTimeSource() {
    this(System.currentTimeMillis());
  }

  public FakeTimeSource(Date now) {
    this(now.getTime());
  }

  public FakeTimeSource(long now) {
    this.now = now;
  }

  public void advanceSeconds(long seconds) {
    now += seconds * 1000L;
  }

  @Override
  public long currentTimeMillis() {
    return now;
  }
}
