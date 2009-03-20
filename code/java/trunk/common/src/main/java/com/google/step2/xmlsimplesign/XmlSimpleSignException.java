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
package com.google.step2.xmlsimplesign;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

public class XmlSimpleSignException extends Exception {

  public XmlSimpleSignException(String msg) {
    super(msg);
  }

  public XmlSimpleSignException(Throwable cause) {
    super(cause);
  }

  public XmlSimpleSignException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public XmlSimpleSignException(String msg, Document xml) {
    super(msg + ": " + new XMLOutputter().outputString(xml));
  }
}
