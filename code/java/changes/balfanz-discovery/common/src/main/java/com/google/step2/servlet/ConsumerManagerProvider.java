/**
 * Copyright 2008 Google Inc.
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

package com.google.step2.servlet;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.openid4java.consumer.ConsumerAssociationStore;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;

/**
 * 
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
@Singleton
public class ConsumerManagerProvider implements Provider<ConsumerManager> {
  private ConsumerManager manager;

  @Inject
  public ConsumerManagerProvider(ConsumerAssociationStore assocStore) {
    try {
      ConsumerManager manager = new ConsumerManager();
      manager.setAssociations(assocStore);
      this.manager = manager;
    } catch (ConsumerException e) {
      throw new InstantiationError(e.getMessage());
    }
  }

  public ConsumerManager get() {
    return manager;
  }
}
