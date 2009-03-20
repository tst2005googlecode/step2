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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Represents a host-meta file for a certain site (host).
 */
public class HostMeta {

  private final static Logger log = Logger.getLogger(HostMeta.class.getName());

  // links found in the host-meta
  private final ArrayList<Link> links;

  // link-patterns found in the host-meta
  private final ArrayList<LinkPattern> linkPatterns;

  /**
   * Returns a host-meta, as read and parsed from a stream.
   * @throws IOException if we can't read from the stream.
   */
  public static HostMeta parseFromStream(InputStream content)
      throws IOException {

    HostMeta result = new HostMeta();
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(content, "UTF-8"));

    int i = 0;
    String line;

    while ((line = reader.readLine()) != null) {
      i++;
      line = line.trim();

      // read over comments
      if (line.startsWith("#")) {
        continue;
      }

      // read over empty lines
      if (line.length() == 0) {
        continue;
      }

      try {

        if (line.toLowerCase().startsWith("link:")) {
          result.addLink(Link.fromString(line));
        } else if (line.toLowerCase().startsWith("link-pattern:")) {
          result.addLinkPattern(LinkPattern.fromString(line));
        } else {
          log.info("ignoring line in host-meta: " + line);
        }

      } catch (LinkSyntaxException e) {
        // couldn't parse line. Maybe it wasn't a _link_ line?
        log.warning("could not parse line " + i + " in host-meta: " +
            line);
      }
    }

    return result;
  }

  /**
   * Parses a host-meta from a byte array.
   */
  public static HostMeta parseFromBytes(byte[] bytes) {
    try {
      return parseFromStream(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      // this should never happen
      throw new RuntimeException(e);
    }
  }

  public HostMeta() {
    links = new ArrayList<Link>();
    linkPatterns = new ArrayList<LinkPattern>();
  }

  public Collection<Link> getLinks() {
    return links;
  }

  public Collection<LinkPattern> getLinkPatterns() {
    return linkPatterns;
  }

  public void addLink(Link link) {
    links.add(link);
  }

  public void addLinkPattern(LinkPattern linkPattern) {
    linkPatterns.add(linkPattern);
  }
}
