
<%@page import="com.google.gdata.data.Link"%>
<%@page import="com.google.gdata.data.contacts.ContactEntry"%>
<%@page import="com.google.gdata.data.contacts.ContactFeed"%>

<%
  boolean longVersion = !"short".equals(request.getParameter("size"));
%>

<% if (longVersion) { %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>
<head>
<title>Results of Step2 Authentication and Authorization</title>
<link rel="stylesheet" href="style.css" type="text/css" />
</head>
<body>
<h2>Authentication Successful</h2>

<h3>Open ID Results:</h3>
<ul>
<li>Your Open ID is:<br/> ${user}</li>
</ul>

<% } %>

<div id="results">

<h3>Attribute Exchange Results:</h3>
<ul>
<li>AX Fetch Email Response : ${email}</li>
<li>AX Fetch Country Response: ${country}</li>
<li>AX Fetch Language Response: ${language}</li>
<li>AX Fetch First Name Response: ${firstName}</li>
<li>AX Fetch Last Name Response: ${lastName}</li>
<li>AX Validated Email: ${emailval}</li>
</c:if>
</ul>

<% if (longVersion) { %>

<h3>OAuth Extension Results:</h3>
<p>If an authorized request token was returned, this will try to exchange it
for an access token automatically:</p>

<ul>
<li>Authorized OAuth request token: ${request_token} </li>
<li>Oauth access token: ${access_token}</li>
<li>Oauth access token secret: ${access_token_secret}</li>
</ul>

<% } %>

<%
    ContactFeed resultFeed = (ContactFeed)request.getAttribute("contacts");

    if (resultFeed != null) {
%>
<h3>Contacts</h3>

<ul>
<%
      for (ContactEntry entry : resultFeed.getEntries()) {
        String name = entry.getTitle().getPlainText();
        if (name == null || name.length() == 0) {
          continue;
        }
%>

<li> <%= name %> </li>

<%
      }
%>
</ul>

<%
    }
%>

<a href="?logout">Logout</a>
</div>

<% if (longVersion) { %>

</body>
</html>

<% } %>
