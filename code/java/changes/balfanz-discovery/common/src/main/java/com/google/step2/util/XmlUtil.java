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

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class for parsing XML documents. It can parse a document either into an
 * org.w3c.dom.Document or a org.jdom.Document. In either case, it uses a
 * parser that is configured to not touch the file system or network while
 * parsing the XML.
 */
public class XmlUtil {

  public static org.w3c.dom.Document getDocument(InputStream input)
      throws SAXException, IOException, ParserConfigurationException {

    DocumentBuilderFactory factory = getSecureDocumentBuilderFactory();
    DocumentBuilder builder = factory.newDocumentBuilder();

    return builder.parse(input);
  }

  public static Document getJdomDocument(InputStream input)
      throws JDOMException, IOException {
    try {
      return new DOMBuilder().build(getDocument(input));
    } catch (SAXException e) {
      throw new JDOMException("couldn't parse xml", e);
    } catch (ParserConfigurationException e) {
      throw new JDOMException("could not configure parser", e);
    }
  }

  /* visible for testing */
  static DocumentBuilderFactory getSecureDocumentBuilderFactory()
     throws ParserConfigurationException {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    factory.setNamespaceAware(true);
    factory.setFeature(
      "http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature(
      "http://xml.org/sax/features/external-parameter-entities",false);
    factory.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    return factory;
  }
}
