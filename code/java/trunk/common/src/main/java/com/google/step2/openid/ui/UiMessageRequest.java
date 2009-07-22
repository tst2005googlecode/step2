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

import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;

import java.util.Locale;

public class UiMessageRequest extends UiMessage {

  private String language = null;
  private UiMode mode = null;
  private Boolean icon = null;

  public ParameterList getParameters() {
    ParameterList list = new ParameterList();
    if (language != null) {
      list.set(new Parameter(UiMessage.LANG_KEY, language));
    }
    if (mode != null) {
      list.set(new Parameter(UiMessage.MODE_KEY, mode.getMode()));
    }
    if (icon != null) {
      list.set(new Parameter(UiMessage.ICON_KEY, icon.toString()));
    }
    return list;
  }

  public void setParameters(ParameterList params) {
    setLanguage(params);
    setMode(params);
    setIcon(params);
  }

  public void setLanguage(Locale locale) {
    language = locale.toString();
  }

  public void setUiMode(UiMode uiMode) {
    mode = uiMode;
  }

  public void setIconRequest(boolean requestIcon) {
    icon = requestIcon;
  }

  private void setIcon(ParameterList params) {
    String iconValue = params.getParameterValue(UiMessage.ICON_KEY);
    if (iconValue == null || iconValue.trim().equals("")) {
      return;
    }
    icon = Boolean.valueOf(iconValue);
  }

  private void setMode(ParameterList params) {
    String modeString = params.getParameterValue(UiMessage.MODE_KEY);
    if (modeString == null || modeString.trim().equals("")) {
      return;
    }
    if (modeString.equals(UiMode.POPUP.getMode())) {
      mode = UiMode.POPUP;
    }
  }

  private void setLanguage(ParameterList params) {
    String languageString = params.getParameterValue(UiMessage.LANG_KEY);
    if (languageString == null || languageString.trim().equals("")) {
      return;
    }
    language = languageString;
  }
}
