<%@ page session="true" %>
<%@ page import="org.openid4java.message.ParameterList,
                 org.openid4java.message.Message,
                 org.openid4java.message.MessageExtension,
                 org.openid4java.message.MessageExtensionFactory,
                 org.openid4java.message.Parameter,
                 com.google.step2.openid.ax2.AxMessage2,
                 com.google.step2.hybrid.HybridOauthMessage,
                 com.google.step2.example.provider.Step2OAuthProvider,
                 net.oauth.OAuthAccessor,
                 java.util.Set" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<title>Provider Authentication and Authorization</title>
<link rel="stylesheet" href="style.css" type="text/css" />
</head>
<body>
<%
  ParameterList requestParams =
    (ParameterList) session.getAttribute("parameterlist");
  String realm = requestParams.getParameterValue("openid.realm");
  String returnTo = requestParams.getParameterValue("openid.return_to");
  String claimedId = requestParams.getParameterValue("openid.claimed_id");
  String identity = requestParams.getParameterValue("openid.identity");
  
  String provider = request.getScheme() + "://" + request.getServerName() +
    ":" + request.getServerPort() + "/step2-example-provider/";
  String username = null;
  
  if (identity != null & identity.startsWith(provider)) {
    username = identity.substring(provider.length());  
  }

  Message message = Message.createMessage(requestParams);
%>
<h2>Provider Authentication and Authorization</h2>

<ul>
<%
  for (Object p : requestParams.getParameters()) {
    String value = p.toString();
%>
  <li><%= value %></li>
<%
  }
%>
</ul>

<%
  String action = request.getParameter("action");
  if (action == null) {
    String site = realm == null ? returnTo : realm;
%>
<div>
  <b>ClaimedID:</b>
  <pre><%= claimedId%></pre>
</div>

<div>
  <b>Identity:</b>
  <pre><%= identity %></pre
</div>

<div>
  <b>Site:</b>
  <pre><%= site %></pre
</div>

<% if (username != null && username.length() > 0) { %>
<div>
  <b>Username:</b> <%= username %><br/><br/>
</div>
<% } %>

<form action="?action=authorized" method="post">
<%
  if (message.hasExtension(AxMessage2.OPENID_NS_AX_FINAL)) {
    MessageExtension axMessage =
      message.getExtension(AxMessage2.OPENID_NS_AX_FINAL);
    
    if (axMessage.getParameters().hasParameter("type.email")) {
%>  
<div>
  <input type="checkbox" name="email" value="yes" checked />
  Approve email request.
</div>
<% 
    }  // End check for email request
    
    if (axMessage.getParameters().hasParameter("type.country")) {
%>
<div>
  <input type="checkbox" name="country" value="yes" checked />
  Approve country request.
</div>
<%
    }  // End check for country request
  }  // End axMessage block

  if (message.hasExtension(HybridOauthMessage.OPENID_NS_OAUTH)) {
    MessageExtension oauthMessage =
      message.getExtension(HybridOauthMessage.OPENID_NS_OAUTH);
    String requestToken =
      oauthMessage.getParameters().getParameterValue("request_token");
    if (requestToken != null) {
%>
<div>
  <input type="checkbox" name="oauth_request" value="yes" checked />
  Authorize OAuth Request Token
<input type="hidden" name="request_token" value="<%= requestToken %>" />
</div>
<!--  <div>
  <input type="checkbox" name="oauth_access" value="yes" />
  Return OAuth Access Token
</div>
 -->
</div>
<%
    }  // End request_token block  
  }  // End Oauth block
%>
<div>
  <input type="submit" title="Login" id="login" value="Approve"/>
</div>
</form>
<%
  } else {
    String allowEmail = request.getParameter("email");        
    if ("yes".equals(allowEmail)) {
      session.setAttribute("email", "foo@bar.com");      
    }
      
    String allowCountry = request.getParameter("country");
    if ("yes".equals(allowCountry)) {
      session.setAttribute("country", "us");
    }
    String requestToken = request.getParameter("request_token");
    String allowOauthRequest = request.getParameter("oauth_request");
    if (requestToken != null) {
      if ("yes".equals(allowOauthRequest)) {
        // Authorize this user's request token
        Step2OAuthProvider.authorizeAccessor(requestToken);
        /*
        String allowOauthAccess = request.getParameter("oauth_access");
        
        if ("yes".equals(allowOauthAccess)) {
          // Return an access token
          OAuthAccessor accessor = Step2OAuthProvider.generateAccessToken(requestToken);
          session.removeAttribute("oauth_request_token");
          session.setAttribute("oauth_access_token", accessor.accessToken);
          session.setAttribute("oauth_access_token_secret", accessor.tokenSecret);
        } else {
          // Just return an authorized request token
          */
          session.setAttribute("oauth_request_token", requestToken);
        //}
      }
    }
    
    session.setAttribute("authenticatedAndApproved", Boolean.TRUE);
    response.sendRedirect("openid?_action=complete");
  }
%>
