package com.google.step2.example.provider.servlet;

import com.google.step2.Step2;
import com.google.step2.openid.ax2.FetchResponse2;

import org.openid4java.message.AuthSuccess;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.server.ServerManager;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class DummyOpenIDServlet extends HttpServlet {
  // instantiate a ServerManager object
  ServerManager manager = new ServerManager();
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest httpReq, HttpServletResponse httpResp)
      throws IOException, ServletException {
    manager.setOPEndpointUrl("http://localhost:8081/step2-example-provider/openid");

    HttpSession session = httpReq.getSession();
    // extract the parameters from the request
    ParameterList requestParams;
    if ("complete".equals(httpReq.getParameter("_action"))) {
      requestParams = (ParameterList) session.getAttribute("parameterlist"); 
    } else {
      requestParams = new ParameterList(httpReq.getParameterMap());
    }
    
    String mode = requestParams.getParameterValue("openid.mode");
    StringBuffer responseText = new StringBuffer();
    Message responseMessage;

    if ("associate".equals(mode)) {
      // --- process an association request ---
      responseMessage = manager.associationResponse(requestParams);
      responseText.append(responseMessage.keyValueFormEncoding());
    } else if ("checkid_setup".equals(mode) ||
        "checkid_immediate".equals(mode)) {

      Boolean authenticatedAndApproved = Boolean.FALSE;
      if (session.getAttribute("authenticatedAndApproved") != null) {
        authenticatedAndApproved =
          (Boolean) session.getAttribute("authenticatedAndApproved");
      }
      
      if (!authenticatedAndApproved) {
        // Interact with the user and obtain data needed to continue
        session.setAttribute("parameterlist", requestParams);
        httpResp.sendRedirect("authorize");
        return;
      }
      
      String userId = (String) session.getAttribute("openid.claimed_id");
      String userClaimedId = (String) session.getAttribute("openid.identity");
      // Remove the parameterlist so this provider can accept any request
      session.removeAttribute("parameterlist");
      // Makes you authorize each and every time
      session.setAttribute("authenticatedAndApproved", Boolean.FALSE); 

      // Process an authorization event
      responseMessage = manager.authResponse(requestParams, userId,
          userClaimedId, authenticatedAndApproved.booleanValue());

      if (responseMessage instanceof AuthSuccess) {        
        FetchResponse2 fetchResponse = new FetchResponse2();
        String email = (String) session.getAttribute("email");
        if (email != null) {
          fetchResponse.addAttribute("email", Step2.AX_EMAIL_SCHEMA, email);
        }

        String country = (String) session.getAttribute("country");
        if (country != null) {
          fetchResponse.addAttribute("country", Step2.AX_COUNTRY_SCHEMA, country);
        }
        try {
          responseMessage.addExtension(fetchResponse);
        } catch (MessageException e) {
          throw new ServletException(e);
        }
        httpResp.sendRedirect(
            ((AuthSuccess) responseMessage).getDestinationUrl(true));
        return;
      }
      responseText.append("<pre>");
      responseText.append(responseMessage.keyValueFormEncoding());
      responseText.append("</pre>");
    }
    OutputStream os = httpResp.getOutputStream();
    os.write(responseText.toString().getBytes());
    os.close();
  }
}
