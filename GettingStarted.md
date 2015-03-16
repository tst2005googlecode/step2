You'll need maven2 to check out, compile, and run the server.

  * Check out the code:
```
$ svn checkout http://step2.googlecode.com/svn/code/java/trunk/ step2
```
  * Build the server:
```
$ cd step2
$ mvn install
```
  * Start the server:
```
$ cd example-consumer
$ mvn jetty:run
```

The server should now be running on localhost:8080