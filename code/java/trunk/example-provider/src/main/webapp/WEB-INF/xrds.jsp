<%@ page contentType="application/xrds+xml"%><?xml version="1.0" encoding="UTF-8"?>
<% String uriPrefix = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort(); %>
<xrds:XRDS
  xmlns:xrds="xri://$xrds"
  xmlns:openid="http://openid.net/xmlns/1.0"
  xmlns:oauth="http://oauth.net/core/1.0"
  xmlns="xri://$xrd*($v*2.0)">
  <XRD>
      <Service priority="0">
      <Type>http://openid.net/signon/1.0</Type>
      <URI><%= uriPrefix %>/step2-example-provider/openid</URI>
    </Service>
  </XRD>
  <XRD  xml:id="oauth" xmlns:simple="http://xrds-simple.net/core/1.0" xmlns="xri://$XRD*($v*2.0)" version="2.0">
    <Service priority="0">
      <Type>http://oauth.net/core/1.0/endpoint/request</Type>
      <Type>http://oauth.net/core/1.0/parameters/post-body</Type>
      <Type>http://oauth.net/core/1.0/parameters/uri-query</Type>
      <Type>http://oauth.net/core/1.0/signature/HMAC-SHA1</Type>
      <URI><%= uriPrefix %>/step2-example-provider/request_token</URI>
    </Service>
    <Service priority="0">
      <Type>http://oauth.net/core/1.0/endpoint/access</Type>
      <Type>http://oauth.net/core/1.0/parameters/post-body</Type>
      <Type>http://oauth.net/core/1.0/parameters/uri-query</Type>
      <Type>http://oauth.net/core/1.0/signature/HMAC-SHA1</Type>
      <URI><%= uriPrefix %>/step2-example-provider/access_token</URI>
    </Service>
  </XRD>
  <XRD xmlns="xri://$XRD*($v*2.0)" version="2.0">
      <Type>xri://$xrds*simple</Type>
      <Service priority="10">
        <Type>http://oauth.net/discovery/1.0</Type>
        <URI>#oauth</URI>
      </Service>
  </XRD>
</xrds:XRDS>