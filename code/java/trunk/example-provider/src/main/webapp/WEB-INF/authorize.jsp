<%@ page session="true" %>
<%@ page import="org.openid4java.message.ParameterList,
                 org.openid4java.message.Message,
                 org.openid4java.message.MessageExtension,
                 org.openid4java.message.MessageExtensionFactory,
                 org.openid4java.message.Parameter,
                 com.google.step2.openid.ax2.AxMessage2,
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
  Set s = message.getExtensions();
  String axAlias = message.getExtensionAlias(AxMessage2.OPENID_NS_AX_FINAL);

%>
<h1>Provider Authentication and Authorization</h1>

<%
  for (Object o : s) {
%>
<%= o.toString() %>
<%
  }
%>

<%= message.toString() %>

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
  if (request.getParameter("action") == null) {
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

<form action="?action=authorize" method="post">  
<div>
  <input type="checkbox" name="email" value="yes" checked="true" />
  Approve request for email
</div>
<div>
  <input type="checkbox" name="country" value="yes" checked="true" />
  Approve request for country
</div>
<div>
  <input type="checkbox" name="oauth" value="yes" checked="true" />
  Approve Oauth Request
</div>
<div>
  <input type="submit" title="Login" id="login"/>
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

    // if (allowOauth.equals("yes")) {
    //   Do Oauth stuff
    // }

    session.setAttribute("authenticatedAndApproved", Boolean.TRUE);
    response.sendRedirect("openid?_action=complete");
  }
%>
