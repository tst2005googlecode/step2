<%@ page session="true" %>
<%@ page import="org.openid4java.message.ParameterList,
                 com.google.step2.example.consumer.OAuthConsumerUtil" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>

<head>
<title>Example Step2 Authentication and Authorization</title>
<link rel="stylesheet" href="style.css" type="text/css" />
</head>

<body>

<h1>Example Step2 Authentication and Authorization</h1>

<p>
This example form will authenticate a user though an identity provider and
optionally request user email and country attributes.
</p>

<%
  ParameterList requestParams =
    (ParameterList) session.getAttribute("parameterlist");
  if (requestParams != null) {
  String errorMessage = requestParams.getParameterValue("errormessage");
  if (errorMessage != null && errorMessage.length() > 0) {
    System.out.println(errorMessage);
%>
  <p>An error occurred: <%= errorMessage %></p>
<%    
    }
  }
%>

<form method="post">  
<div>
  OpenID URL: <input type="text" id="openid" name="openid" size="50" />
</div>
<div>
  <input type="radio" name="emailmode" value="validate">Email to Validate: <input type="text" id="emailval" name="emailval" size="50" /><br/>
  <input type="radio" name="emailmode" value="request">Request email
</div>
<div>
  <input type="checkbox" name="country" value="yes" />Request home country
</div>
<div>
  <input type="checkbox" name="usePost" value="yes" />Use POST instead of GET
</div>
<div>
  <input type="checkbox" name="oauth" value="yes" />Authorize OAuth Request Token
</div>

<div>
  <input type="submit" title="Login" />
</div>
</form>
</body>
</html>