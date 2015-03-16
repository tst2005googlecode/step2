# How to check out Step 2 from Eclipse 3.3 #

## Requirements ##

  * Java 1.5 or greater
  * Subversion 1.4 or greater
  * Maven 2.0.9 or greater
  * Eclipse 3.3 or greater
  * Subclipse 1.5 or greater
  * m2Eclipse 0.9.5 or greater

## SVN Instructions ##

  1. Select File->Import
  1. From the Import dialog, select Other->"Check out Maven Projects from SCM"
  1. Select "svn" from the SCM URL drop down.
  1. Enter "https://step2.googlecode.com/svn/code/java/trunk" in the URL field.
  1. Select "Finish"

## Adding Step 2 as a Dependency ##

To add Step 2 as a dependency to another project, you can use the following Maven identifiers:
  * Repository: http://step2.googlecode.com/svn/code/java/maven
  * Group-Id: com.google.step2
  * Artifact-Id: step2-parent

You can add these directly to a pom.xml file as follows:
```
<dependencies>
  ...
  <dependency>
    <groupId>com.google.step2</groupId>
    <artifactId>step2-parent</artifactId>
    <version>1-SNAPSHOT</version>
  </dependency>
  ...
</dependencies>
...
<repositories>
  ...
  <repository>
    <id>step2</id>
    <name>Step 2 Maven Repository</name>
    <url>http://step2.googlecode.com/svn/code/java/maven</url>
  </repository>
  ...
</repositories>
```

These instructions are written for Eclipse 3.3 (Europa). Eclipse 3.4 (Ganymede) may differ.