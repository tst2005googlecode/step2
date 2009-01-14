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

package com.google.step2.example.consumer;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Scopes;
import com.google.step2.consumer.OAuthProviderInfoStore;
import com.google.step2.hybrid.HybridOauthMessage;
import com.google.step2.openid.ax2.AxMessage2;
import com.google.step2.servlet.ConsumerManagerProvider;

import net.oauth.client.OAuthClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.openid4java.consumer.ConsumerAssociationStore;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.util.HttpClientFactory;

import java.net.URL;

/**
 *
 * @author Dirk Balfanz (dirk.balfanz@gmail.com)
 * @author Breno de Medeiros (breno.demedeiros@gmail.com)
 */
public class GuiceModule extends AbstractModule {
  @Override
  protected void configure() {

    try {
      Message.addExtensionFactory(AxMessage2.class);
    } catch (MessageException e) {
      throw new CreationException(null);
    }

    try {
      Message.addExtensionFactory(HybridOauthMessage.class);
    } catch (MessageException e) {
      throw new CreationException(null);
    }

    bind(ConsumerAssociationStore.class)
        .to(InMemoryConsumerAssociationStore.class)
        .in(Scopes.SINGLETON);

    bind(ConsumerManager.class)
        .toProvider(ConsumerManagerProvider.class)
        .in(Scopes.SINGLETON);

    bind(HttpClient.class).toInstance(HttpClientFactory.getInstance(0,
        Boolean.FALSE, 10000, 10000, CookiePolicy.IGNORE_COOKIES));

    bind(OAuthClient.class).toInstance(getOAuthClient());

    bind(OAuthProviderInfoStore.class)
        .to(SimpleProviderInfoStore.class).in(Scopes.SINGLETON);
  }

  private OAuthClient getOAuthClient() {
    return new net.oauth.client.httpclient3.OAuthHttpClient();        
  }
}
