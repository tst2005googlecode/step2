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

public abstract class UiMessage implements MessageExtension {

  public enum UiMode {
    POPUP("popup");

    private String mode;

    private UiMode(String mode) {
      this.mode = mode;
    }

    public String getMode() {
      return mode;
    }
  }

  public static final String ICON_KEY = "icon";
  public static final String LANG_KEY = "lang";
  public static final String MODE_KEY = "mode";

  public String getTypeUri() {
    return UiMessageExtension.UI_1_0;
  }

  public boolean providesIdentifier() {
    return false;
  }

  public boolean signRequired() {
    return true;
  }
}
