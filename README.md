Burrito Point of Service REST Web Service
================================

Overview
-------------------------
This is simple Point of Service REST web service for a small town burrito joint. 
This project builds off of the earlier work with the Burrito POS Server.
It serves as a starting point to refresh memory of or learn about new/existing languages, frameworks, design patterns, etc.

Currently demonstrates
-------------------------
* Eclipse
* Java
* Maven
* JUnit
* Spring
* Jersey (JAX-RS)
* OAuth (via Spring Security)
* Mongo (NoSQL)
* Activiti (PostGRES backend)
* RabbitMQ (AMQP)
* Yammer.metrics
* JavaDoc
* Swagger REST API Documentation
  
TODO
-------------------------
* Update/Complete unit tests
* Update/Complete documentation
* Update requirements to demonstrate Activiti, RabbitMQ (AMQP), and Solr or ElasticSearch

Notes
-------------------------
* To be deployed to Apache Tomcat

Be sure the CATALINA_HOME environment variable is correctly configured before building and double-check Tomcat specific variables in the pom.xml.  To deploy:

```mvn clean install```

* BurritoPOSServer.jar installed locally: 

```mvn install:install-file -Dfile=BurritoPOSServer.jar -DgroupId=com.burritopos.server 
-DartifactId=neatoBurrito.server -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar```

Activiti will automatically setup the necessary schema so no pre-initialization of tables are necessary as long as a blank table named 'activiti' exists in PostGRES.

BurritoPOS web service utilizes the [yammer.metrics](http://metrics.codahale.com/) library to provide service metrics and diagnostics.
	
The following servlets are initialized with the burritopos web service and can be accessed by using the relative URL shown.
		
 * Health Check servlet
 	* /burritopos-service/healthcheck
 * Thread Dump servlet
 	* /burritopos-service/threads
 * Metrics servlet
 	* /burritopos-service/metrics
 * Ping servlet
 	* /burritopos-service/ping

BurritoPOS web service utilizes [Swagger](https://github.com/wordnik/swagger-core/wiki) for REST API Documentation
 
The default servlet is initialized at the following URL:
 
 * API servlet
 	* /burritopos-service/api