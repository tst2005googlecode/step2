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
package com.google.step2.discovery;

import static org.easymock.classextension.EasyMock.expect;

import com.google.step2.http.FetchRequest;
import com.google.step2.http.FetchResponse;
import com.google.step2.http.HttpFetcher;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.UrlIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class LegacyXrdsResolverTest extends TestCase {

  private static String SITE_XRD =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns:openid=\"http://openid.net/" +
        "xmlns/1.0\" xmlns=\"xri://$xrd*($v*2.0)\">\n" +
    "<XRD>\n" +
    "<CanonicalID>balfanz.net</CanonicalID>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://specs.openid.net/auth/2.0/server</Type>\n" +
    "<Type>http://openid.net/srv/ax/1.0</Type>\n" +
    "<URI>https://www.google.com/a/balfanz.net/o8/ud?be=o8</URI>\n" +
    "</Service>\n" +
    "<Service priority=\"0\">\n" +
    "<Type>http://www.iana.org/assignments/relation/describedby</Type>\n" +
    "<MediaType>application/xdrs+xml</MediaType>\n" +
    "<URITemplate>https://www.google.com/accounts/o8/user-xrds?uri={%uri}" +
        "</URITemplate>\n" +
    "</Service>\n" +
    "</XRD>\n" +
    "</xrds:XRDS>\n";

  private IMocksControl control;
  private Discovery yadis;
  private HttpFetcher fetcher;

  private ServerSocket socket;
  private int listenPort;
  private ConnectListener connectListener;
  private Thread listenerThread;
  private LegacyXrdsResolver xrdResolver;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createControl();
    yadis = control.createMock(Discovery.class);
    fetcher = control.createMock(HttpFetcher.class);
    xrdResolver = new LegacyXrdsResolver(yadis, fetcher);

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

  public void testFindOpEndpointsForSite() throws Exception {
    IdpIdentifier host = new IdpIdentifier("host");
    URI siteXrdsUri = URI.create("http://example.com/xrds");
    List<DiscoveryInformation> infos = Arrays.asList(
        // a server info
        new DiscoveryInformation(new URL("http://example.com/op1")),
        // a signon info
        new DiscoveryInformation(
            new URL("http://example.com/op2"),
            new UrlIdentifier("http://bob.com")));

    expect(yadis.discover(siteXrdsUri.toString())).andReturn(infos);

    control.replay();
    List<DiscoveryInformation> result =
        xrdResolver.findOpEndpoints(host, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    DiscoveryInformation info = result.get(0);
    assertEquals("http://example.com/op1", info.getOPEndpoint().toString());
  }

  public void testFindOpEndpointsForUser() throws Exception {
    UrlIdentifier user = new UrlIdentifier("http://bob.com/id");
    URI siteXrdsUri = URI.create("http://example.com/xrds");
    List<DiscoveryInformation> infos = Arrays.asList(
        // a server info
        new DiscoveryInformation(new URL("http://example.com/op1")),
        // a signon info
        new DiscoveryInformation(new URL("http://example.com/op2"), user));

    FetchRequest httpRequest = FetchRequest.createGetRequest(siteXrdsUri);

    expect(fetcher.fetch(httpRequest)).andReturn(new FakeResponse(SITE_XRD));
    String userXrdsUri = "https://www.google.com/accounts/o8/user-xrds?uri="
      + URLEncoder.encode(user.getIdentifier(), "UTF-8");
    expect(yadis.discover(userXrdsUri)).andReturn(infos);

    control.replay();
    List<DiscoveryInformation> result =
        xrdResolver.findOpEndpoints(user, siteXrdsUri);
    control.verify();

    assertEquals(1, result.size());
    DiscoveryInformation info = result.get(0);
    assertEquals("http://example.com/op2", info.getOPEndpoint().toString());
    assertEquals(user.getIdentifier(), info.getClaimedIdentifier().toString());
  }

  public void testSecurityOfXmlParser() throws Exception {
    DocumentBuilderFactory factory =
        LegacyXrdsResolver.getSecureDocumentBuilderFactory();

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

  private static class FakeResponse implements FetchResponse {

    private final String content;
    private final int status;

    public FakeResponse(String content) {
      this.status = 200;
      this.content = content;
    }

    public InputStream getContentAsStream() {
      return new ByteArrayInputStream(content.getBytes());
    }

    public int getStatusCode() {
      return status;
    }
  }
}
