<%--
 ~ Copyright 2009 Google Inc.
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~
  --%>
 <%@ page session="true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<%@page import="com.google.step2.example.consumer.servlet.LoginViaPopupServlet"%>
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <title>Pop-up Example</title>
  </head>
  <body style = "background-color:#FFFFFF; color:#000000;">
      <%
        String returnToUrl = (String) session.getAttribute(LoginViaPopupServlet.RETURN_TO);
        String realm = (String) session.getAttribute(LoginViaPopupServlet.REALM);
        realm = (null == realm) ? returnToUrl : realm;
        String endpoint = (String) session.getAttribute(LoginViaPopupServlet.OP_ENDPOINT);
        String buttonImage = (String) session.getAttribute(LoginViaPopupServlet.BUTTON_IMAGE);
        String opFriendlyName = (String) session.getAttribute(LoginViaPopupServlet.OP_FRIENDLY);
        String extensionParameters = (String) session.getAttribute(LoginViaPopupServlet.EXTENSION_PARAMS);
      %>
    <script type="text/javascript" src="popuplib.js">
    </script>
    <div id="popupForm">
      <div style="text-align:center;margin-bottom:4px;margin-top:50px;">
        <table style="width:100%;">
          <tr>
            <td>
              <button name = "submit_button" id = "submit_button"
                style = "background-color:transparent;border-width:1px;border-color:#000000;padding:3px;margin-bottom:200px;">
                <img src="<%= buttonImage %>" alt="<%= opFriendlyName %>" style="margin-bottom:-3px;"/>&nbsp; Login using your Google Account
              </button>
              <br />
              For examples on how to use the library, see <a href="http://step2.googlecode.com">step2.googlecode.com</a>
            </td>
            <td>
              Popup library:<br /> <hr style="width:720px;"/>
              <iframe src = "popuplib.js" style="width:700px;height:400px;background-color:#ECECFF;">
              </iframe>
            </td>
          </tr>
        </table>
      </div>
    </div>
    <script type="text/javascript">
      var greetUser = function() {
        window.location = "/hello";
      };
      var extensions = <%= extensionParameters %>;
      var googleOpener = popupManager.createPopupOpener(
          { 'realm' : '<%= realm %>',
            'opEndpoint' : '<%= endpoint %>',
            'returnToUrl' : '<%= returnToUrl %>',
            'onCloseHandler' : greetUser,
            'shouldEncodeUrls' : true,
            'extensions' : extensions });
      document.getElementById("submit_button").onclick = function() {
        googleOpener.popup(500,450);
        return true;
      };
    </script>
  </body>
</html>