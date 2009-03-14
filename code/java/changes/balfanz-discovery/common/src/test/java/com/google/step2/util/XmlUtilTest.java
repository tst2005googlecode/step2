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

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlUtilTest extends TestCase {

  private ServerSocket socket;
  private int listenPort;
  private ConnectListener connectListener;
  private Thread listenerThread;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    try {
      socket = new ServerSocket(0);
      listenPort = socket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException("Could not get listening socket for test.");
    }
    connectListener = new ConnectListener(socket);
    listenerThread = new Thread(connectListener);
    listenerThread.start();
  }

  @Override
  protected void tearDown() {
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        fail("Could not close listening socket after test.");
      }
    }
    if (listenerThread != null) {
      try {
        listenerThread.join();
      } catch (InterruptedException e) {
        fail("Interrupted waiting for listener thread.");
      }
    }
    assertTrue("Checking no HTTP connects.",
               connectListener.getConnectCount() == 0);
  }

  public void testSecurityOfXmlParser() throws Exception {
    DocumentBuilderFactory factory =
        XmlUtil.getSecureDocumentBuilderFactory();

    String[] attacks = new String[] {

      /* The file /dev/tty is used because it will typically cause a hang if
       * opened, thus causing the test to fail. We need to ensure the file is
       * never opened - it is not enough to just check that file contents do not
       * appear in the parsed XML document.
       */
      "<!DOCTYPE doc [ <!ENTITY % ent SYSTEM \"file:///dev/tty\">%ent;]><ele/>",

      /* The file /nosuch/file is used because it should not exist. Here, we are
       * checking that the parser does not behave differently when a
       * non-existant file is specified. We don't want a file existence
       * disclosure attack.
       */
      "<!DOCTYPE doc [ <!ENTITY % ent SYSTEM \"file:///nosuch/file\">%ent;]>" +
          "<ele/>",
      "<!DOCTYPE doc [ <!ENTITY ent SYSTEM \"file:///dev/tty\"> ]>" +
          "<ele>&ent;</ele>",
      "<!DOCTYPE doc [ <!ENTITY ent SYSTEM \"file:///nosuch/file\"> ]>" +
          "<ele>&ent;</ele>",
      "<!DOCTYPE doc SYSTEM \"file:///dev/tty\"><ele/>",
      "<!DOCTYPE doc SYSTEM \"file:///nosuch/file\"><ele/>",
      "<!DOCTYPE doc [ <!ENTITY % ent SYSTEM " +
          "\"http://localhost:" + listenPort + "/abc\">%ent;]><ele/>",
      "<!DOCTYPE doc [ <!ENTITY ent SYSTEM \"http://localhost:" + listenPort +
          "/abc\"> ]><ele>&ent;</ele>",
      "<!DOCTYPE doc SYSTEM \"http://localhost:" + listenPort + "/abc\">" +
          "<ele/>",
      "<!DOCTYPE rss PUBLIC \"-//Netscape Communications//DTD RSS 0.91//EN\" " +
          "\"file:///dev/tty\"><ele/>",
      "<!DOCTYPE rss PUBLIC \"-//Netscape Communications//DTD RSS 0.91//EN\" " +
          "\"http://localhost:" + listenPort + "/abc\"><ele/>"
    };

    for (String attack : attacks) {
      try {
        DocumentBuilder builder = factory.newDocumentBuilder();
        parseStringDOM(builder, attack);
      } catch (SAXException e) {

        // Not expected to occur in this test.
        throw new RuntimeException(e);
      } catch (javax.xml.parsers.ParserConfigurationException e) {

        // Parse errors are expected, ignore them.
      }
    }

    // A couple of little tests to ensure that we didn't break built-in
    // entity resolution for things like &amp;
    String inbuiltEntities =
        "<ele att=\"&amp;&#9;&#x3f;\">&amp;&#9;&#x3f;</ele>";
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = parseStringDOM(builder, inbuiltEntities);
      Element rootEle = doc.getDocumentElement();
      assertEquals("&\t?", rootEle.getAttribute("att"));
      assertEquals("&\t?", rootEle.getFirstChild().getNodeValue());
    } catch (ParserConfigurationException e) {

      // Not expected to occur in this test.
      throw new RuntimeException(e);
    } catch (SAXException e) {

      // Not expected to occur in this test.
      throw new RuntimeException(e);
    }

    String benignDTDTest =
        "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" +
        "<!DOCTYPE rss PUBLIC " +
        "\"-//Netscape Communications//DTD RSS 0.91//EN\" " +
        "\"http://my.netscape.com/publish/formats/rss-0.91.dtd\"><ele/>";
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      parseStringDOM(builder, benignDTDTest);
    } catch (SAXException e) {

      // Test failed - benign DTD should have parsed without error.
      throw new RuntimeException(e);
    } catch (ParserConfigurationException e) {

      // Not expected to occur in this test.
      throw new RuntimeException(e);
    }
  }

  /* Parses a string with the current DOM parser. */
  private Document parseStringDOM(DocumentBuilder builder, String xml)
      throws SAXException {

    Reader reader = new StringReader(xml);
    InputSource source = new InputSource(reader);
    try {
      return builder.parse(source);
    } catch (IOException e) {

      // Not expected to occur in this test.
      throw new RuntimeException(e);
    }
  }

  /**
   *  Monitors the pretend HTTP listener for errant connects.
   */
  static private class ConnectListener implements Runnable {

    // Number of errant connections received.
    private int connectCount;

    // The listening socket.
    private ServerSocket socket;

    ConnectListener(ServerSocket s) {
      socket = s;
      connectCount = 0;
    }

    // Runs the main pretend HTTP listener loop.
    public void run() {
      do {
        Socket newSocket = null;
        try {
          newSocket = socket.accept();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        connectCount++;
        try {
          newSocket.close();
        } catch (IOException e) {

          // Ignore - the connect was logged which will cause failure later.
        }
      } while (true);
    }

    // Gets the number of errant connects logged.
    int getConnectCount() {
      return connectCount;
    }
  }
}
