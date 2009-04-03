<%@ page session="true" %>
<%@ page import="org.openid4java.message.ParameterList,
                 com.google.step2.example.consumer.OAuthConsumerUtil" %>

<html>

<head>
<title>Example Step2 Authentication and Authorization</title>
<link rel="stylesheet" href="style.css" type="text/css" />

<script type="text/javascript" src="jquery-1.3.1.js"></script>

<script type="text/javascript">
  var state = "discovery";

  function disableLoginForm() {
      $("#openid").attr("disabled", "disabled");
      $("#submit > input").attr("disabled", "disabled");
      $("#spinner").css("display", "block");
  }

  function enableLoginForm() {
      $("#openid").removeAttr("disabled");
      $("#submit > input").removeAttr("disabled");
      $("#spinner").css("display", "none");
  }

  function setupPasswordLogin() {
    state = "password";
    enableLoginForm();
    $("#password-div").css("position", "relative")
    $("#password-div").css("top", "-0px")
    $("#password").removeAttr("tabindex");
    $("#password")[0].focus();
    $("#submit-button").attr("value", "Sign in");
  }

  function setupDiscoveredLogin() {
    state = "discovery";
    enableLoginForm();
    $("#password").attr("tabindex", "9999");
    $("#password-div").css("position", "absolute")
    $("#password-div").css("top", "-5000px")
    $("#openid")[0].focus();
    $("#submit-button").attr("value", "Continue");
  }

  function startDiscovery() {
    disableLoginForm();
    $.post("lso", {
        openid: $("#openid").val(),
        email: $("#email").attr("checked") ? "yes" : "no",
        country: $("#country").attr("checked") ? "yes" : "no",
        language: $("#language").attr("checked") ? "yes" : "no",
        firstName: $("#firstName").attr("checked") ? "yes" : "no",
        lastName: $("#lastName").attr("checked") ? "yes" : "no",
        usePost: $("#usePost").attr("checked") ? "yes" : "no",
        oauth: $("#oauth").attr("checked") ? "yes" : "no",
        stage: "discovery"
    },
    function(data) {
      if (data.status === "error") {
        setupPasswordLogin();
      } else if (data.status === "success") {
        document.location = data.redirectUrl;
      } else {
        alert("got weird response from server");
        enableLoginForm();
      }
    }, "json");
  }

  $(document).ready(function() {
    $("form").submit(function(e) {
      if (state === "discovery") {
          e.preventDefault();
          startDiscovery();
      } // else we don't consume the event and the form
        // gets submitted.
    });
    setupDiscoveredLogin();
  });
</script>

</head>

<body>

<h1>Example Step2 Authentication and Authorization</h1>

<p>
This example form will authenticate a user though an identity provider and
optionally request user email and country attributes.</p>
<p>
If your email provider is an OpenID IdP you will be taken to your IdP.
Otherwise, you'll be asked for a password. <b>You can type any password you
like at that point. DONT USE A REAL PASSWORD!!!</b>
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

<form id="form" method="post" action="/lso">
<div id="loginform">
  <div id="preamble">
  Sign in with your<br/>
  <b>Email Address:</b>
  </div>
  <div id="email-div">
    <label for="openid">Email: </label>
    <input type="text" id="openid" name="openid" size="20" />
  </div>
  <div id="password-div">
    <label for="password">Password: </label>
    <input type="password" id="password" name="password" size="20" />
  </div>
  <div id="submit">
    <input id="submit-button" type="submit" value="Continue"/>
    <img id="spinner" src="ajax-loader.gif" style="display: none;" />
  </div>
  <div style="clear:both;"></div>
</div>
<p>
<div>
  <input id="email" type="checkbox" name="email" value="yes" />AX Request email
</div>
<div>
  <input id="country" type="checkbox" name="country" value="yes" />AX Request home country
</div>
<div>
  <input id="language" type="checkbox" name="language" value="yes" />AX Request preferred language
</div>
<div>
  <input id="firstName" type="checkbox" name="firstName" value="yes" />AX Request first name
</div>
<div>
  <input id="lastName" type="checkbox" name="lastName" value="yes" />AX Request last name
</div><hr />
<div>
  <input id="usePost" type="checkbox" name="usePost" value="yes" />Use POST instead of GET
</div>
<div>
  <input id="oauth" type="checkbox" name="oauth" value="yes" />Get OAuth Request token, then authorize
</div>
</form>

</body>
</html>
