<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<title>Results of Step2 Authentication and Authorization</title>
<link rel="stylesheet" href="style.css" type="text/css" />
</head>
<body>
<h2>Authentication Successful</h2>
<p>Your Open ID is:<br/> ${user}</p>

<h2>Authorization Results</h2>
<div> Your email is: ${email}. </div> 
<div> Your country is: ${country}. </div>
<div> Your Oauth Access Token is: ${token}. </div>

<a href="?logout">Logout</a>

</body>
</html>
