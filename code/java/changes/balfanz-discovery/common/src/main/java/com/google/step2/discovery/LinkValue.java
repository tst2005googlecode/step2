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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class that represents a link-value, i.e., the stuff to the right of the
 * colon in a Link: or Link-Pattern: HTTP header (also used in host-meta files).
 *
 * Link: <http://example.com>; rel=rel-param; other-param=other-value
 *        |---uri-string---|   |-----------parameters---------------|
 *       |---------------------link-value---------------------------|
 *
 * We parse out the uri-string and the link parameters. Special attention
 * is given to the "rel" parameter, which in turn is parsed into a
 * RelTypes datastructure.
 */
public class LinkValue {

  public static LinkValue fromString(String link) throws LinkSyntaxException {
    return new Parser(link).link_value();
  }

  private static Pattern WHITESPACE = Pattern.compile("\\s");

  private final String uri;
  private final RelTypes relTypes;
  private final Map<String, String> params;

  private LinkValue(String uri, RelTypes relTypes, Map<String, String> params) {
    this.uri = uri;
    this.relTypes = relTypes;
    this.params = params;
  }

  public RelTypes getRelationships() {
    return relTypes;
  }

  protected String getUriString() {
    return uri;
  }

  public String getMimeType() {
    return params.get("type");
  }

  public String getParameter(String name) {
    return params.get(name);
  }

  /**
   * Hashcode and equals are only based on params and uri.
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((params == null) ? 0 : params.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LinkValue other = (LinkValue) obj;
    if (params == null) {
      if (other.params != null) return false;
    } else if (!params.equals(other.params)) return false;
    if (uri == null) {
      if (other.uri != null) return false;
    } else if (!uri.equals(other.uri)) return false;
    return true;
  }

  /**
   * Allows you to slowly build up a link from its constituent parts.
   * This class is used by the parser.
   */
  private static class Builder {

    private final String uri;
    private final Collection<RelType> relTypes = new ArrayList<RelType>();
    private final Map<String, String> params = new HashMap<String, String>();

    public Builder(String uri) {
      this.uri = uri;
    }

    public Builder addRelType(RelType relType) {
      relTypes.add(relType);
      return this;
    }

    public LinkValue create() {
      return new LinkValue(uri, RelTypes.setOf(relTypes), params);
    }

    public Builder addLinkParameter(String name, String value) {
      params.put(name, value);
      return this;
    }
  }

  /**
   * Parsers that can parse link-values.
   */
  private static class Parser {

    // the line of text we're parsing
    private final String input;

    public Parser(String input) {
      this.input = input.trim();
    }

    // the main entry method. We're using non-standard method name
    // capitalization b/c the method names match the BNF-definition
    // in the spec.
    public LinkValue link_value() throws LinkSyntaxException {

      String s = input;

      if (!s.startsWith("<")) {
        throw new LinkSyntaxException("missing '<': " + input);
      }

      int greaterThan = s.indexOf('>', 1);

      if (greaterThan < 0) {
        throw new LinkSyntaxException("missing '>':" + input);
      }

      String left = s.substring(1, greaterThan).trim();
      String right = s.substring(greaterThan + 1).trim();

      // get the uri string out from between the '<' and '>'
      String uri_reference = uri_reference(left);

      // init a builder with that uri string[
      Builder builder = new Builder(uri_reference);

      // parse the rest of the line (link params), passing the builder
      // along so that we can add params to it as we encounter them.
      link_params(right, builder);

      return builder.create();
    }

    private String uri_reference(String s) throws LinkSyntaxException {
      if (s == null || s.length() == 0) {
        throw new LinkSyntaxException("got empty uri-reference: " + input);
      }
      return s;
    }

    // parses a list of link-params (e.g. "; foo=bar; bla=baz")
    private void link_params(String s, Builder builder)
        throws LinkSyntaxException {

      if (s == null || s.length() == 0) {
        // that's fine - link params are optional;
        return;
      }

      if (!s.startsWith(";")) {
        throw new LinkSyntaxException(
            "link-params must start with ';': " + input);
      }

      // skip over the semicolon;
      String remainder = s.substring(1).trim();

      // parse the first (left-most) link parameter
      link_param(remainder, builder);
    }

    // parses a single link-param out of a list of link-params. In particular,
    // it will parse the left-most link-param and then recursively call
    // link-params (not the plural) to parse the rest of the parameters.
    private void link_param(String s, Builder builder)
        throws LinkSyntaxException {

      int eq = s.indexOf('=');

      if (eq < 0) {
        throw new LinkSyntaxException("missing '=' in link-param:" + input);
      }

      String left = s.substring(0, eq).trim();
      String right = s.substring(eq + 1).trim();

      // we'll be calling param_value to parse the parameter value, which will
      // tell us how much of the remaining input it consumed.
      int consumedUntil;

      // we treat "rel" params special, since we parse their internal
      // structure
      if (left.equalsIgnoreCase("rel")) {
        consumedUntil = relation_type(right, builder);
      } else {
        consumedUntil = param_value(right, builder, left);
      }

      // now, parse the rest of the remaining input agains as a list of params
      String remainder = right.substring(consumedUntil).trim();
      link_params(remainder, builder);
    }

    // returns the length of the prefix that turns out to be a relation-type
    private int relation_type(String s, Builder builder)
        throws LinkSyntaxException {
      if (s.startsWith("\"")) {
        return quoted_relation_types(s, builder);
      } else {
        return unquoted_relation_type(s, builder);
      }
    }

    private int unquoted_relation_type(String s, Builder builder)
        throws LinkSyntaxException {
      if (s == null || s.length() == 0) {
        throw new LinkSyntaxException("missing value in: " + input);
      }

      // this is ended by a semicolon, or by end-of-line
      String[] parts = s.split(";"); // split by semicolon

      // let's also add it as a param, in case people are interested in the
      // unparsed rel param
      builder.addLinkParameter("rel", parts[0].trim());

      RelType relType;
      try {
        relType = new RelType(parts[0].trim());
      } catch (IllegalArgumentException e) {
        // thrown when relType is not a valid URI
        throw new LinkSyntaxException(parts[0].trim() + " is not a valid URI",
            e);
      }
      builder.addRelType(relType);
      return parts[0].length();
    }

    private int quoted_relation_types(String s, Builder builder)
        throws LinkSyntaxException {
      if (!s.startsWith("\"")) {
        throw new LinkSyntaxException("expected \" in relation-type: " + input);
      }

      int secondQuote = s.indexOf('"', 1);

      if (secondQuote < 0) {
        throw new LinkSyntaxException("could not find closing quote in: "
            + input);
      }

      int result = secondQuote + 1; // that's how long the whole thing was

      String sWithoutQuotes = s.substring(1, secondQuote);

      // let's also add it as a param, in case people want to see the
      // unparsed rel parameter
      builder.addLinkParameter("rel", sWithoutQuotes);

      String[] relTypes = sWithoutQuotes.split("\\s"); // split by whitespace

      for (String relTypeString : relTypes) {
        RelType relType;
        try {
          relType = new RelType(relTypeString.trim());
        } catch (IllegalArgumentException e) {
          // thrown when relTypeString is not a valid URI
          throw new LinkSyntaxException(relTypeString.trim() +
              "is not a valid URI", e);
        }
        builder.addRelType(relType);
      }

      return result;
    }

    // returns the length of the prefix that turns out to be a parameter value
    // (the stuff following the prefix are different link parameters)
    private int param_value(String s, Builder builder, String paramName)
        throws LinkSyntaxException {
      if (s.startsWith("\"")) {
        return quoted_param_value(s, builder, paramName);
      } else {
        return unquoted_param_value(s, builder, paramName);
      }
    }

    private int quoted_param_value(String s, Builder builder, String paramName)
        throws LinkSyntaxException {

      if (!s.startsWith("\"")) {
        throw new LinkSyntaxException("expected \" in: " + input);
      }

      int secondQuote = s.indexOf('"', 1);

      if (secondQuote < 0) {
        throw new LinkSyntaxException("could not find closing quote in: "
            + input);
      }

      int result = secondQuote + 1; // that's how long the whole thing was

      String sWithoutQuotes = s.substring(1, secondQuote);

      builder.addLinkParameter(paramName, sWithoutQuotes);

      return result;
    }

    private int unquoted_param_value(String s, Builder builder, String paramName)
        throws LinkSyntaxException {
      if (s == null || s.length() == 0) {
        throw new LinkSyntaxException("missing value in: " + input);
      }

      // this is ended by semicolo, or by end-of-line
      String[] parts = s.split(";"); // split by semicolon

      String paramValue = parts[0].trim();

      if (WHITESPACE.matcher(paramValue).find()) {
        throw new LinkSyntaxException("unexpected whitespace in unqoted param " +
            paramName + " in link value " + input);
      }

      builder.addLinkParameter(paramName, paramValue);
      return parts[0].length();
    }
  }
}
