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
<h1>Provider Authentication and Authorization</h1>

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
  <input type="checkbox" name="email" value="yes" checked="true" />
  Approve email request.
</div>
<% 
    }  // End check for email request
    
    if (axMessage.getParameters().hasParameter("type.country")) {
%>
<div>
  <input type="checkbox" name="country" value="yes" checked="true" />
  Approve country request.
</div>
<%
    }  // End check for country request
  }  // End axMessage block

  if (message.hasExtension(HybridOauthMessage.OPENID_NS_OAUTH)) {
    MessageExtension oauthMessage =
      message.getExtension(HybridOauthMessage.OPENID_NS_OAUTH);
    String token =
      oauthMessage.getParameters().getParameterValue("request_token");
%>
<div>
  <input type="checkbox" name="oauth" value="yes" checked="true" />
  <input type="hidden" name="request_token" value="<%= token %>" />
  Approve Oauth request.
</div>
<%
  }  // End Oauth block
%>
<div>
  <input type="submit" title="Login" id="login" value="Approve"/>
</div>
</form>

<%
  } else {
    String allowEmail = request.getParameter("email");
    String allowCountry = request.getParameter("country");
    String allowOauth = request.getParameter("oauth");
        
    if ("yes".equals(allowEmail)) {
      session.setAttribute("email", "foo@bar.com");      
    }
    
    if ("yes".equals(allowCountry)) {
      session.setAttribute("country", "us");
    }

    if ("yes".equals(allowOauth)) {
      String oauthToken = request.getParameter("request_token");
      session.setAttribute("oauth_token", oauthToken);
      ParameterList params = ParameterList.createFromQueryString(oauthToken);
      String token = params.getParameterValue("oauth_token");
      Step2OAuthProvider.authorizeAccessor(token);
    }

    session.setAttribute("authenticatedAndApproved", Boolean.TRUE);
    response.sendRedirect("openid?_action=complete");
  }
%>
