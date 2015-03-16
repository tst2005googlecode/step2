# Running in Jetty without Tomcat #

To run in other servlet containers:
  1. Remove step2/WEB-INF/classes/jsp
  1. In web.xml, remove all servlet and servlet-mapping under:
```
<!--
Automatically created by Apache Jakarta Tomcat JspC.
```

(by mingfai.ma)