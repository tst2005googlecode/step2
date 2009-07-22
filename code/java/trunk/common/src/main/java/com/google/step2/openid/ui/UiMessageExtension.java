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
package com.google.step2.openid.ui;

import org.openid4java.message.MessageExtension;
import org.openid4java.message.MessageExtensionFactory;
import org.openid4java.message.ParameterList;

public class UiMessageExtension implements MessageExtensionFactory {

  public static final String UI_1_0 =
      "http://specs.openid.net/extensions/ui/1.0";

  public MessageExtension getExtension(ParameterList parameterList, boolean isRequest) {
    MessageExtension result;
    if (isRequest) {
      result = new UiMessageRequest();
    } else {
      result = new UiMessageResponse();
    }
    result.setParameters(parameterList);
    return result;
  }

  public String getTypeUri() {
    return UI_1_0;
  }
}
