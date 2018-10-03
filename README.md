# Axon Framework CDI Support

This [Axon Framework](https://axoniq.io) module provides support for the [CDI](http://cdi-spec.org) programming model. It is a CDI portable extension integrating the Axon Framework and providing some intelligent defaults while still allowing for configuring overrides.

The current minimum supported versions are:

* Axon Framework 3.3.5
* CDI 1.2/Java EE 7
* Java SE 8
 
We have so far tested sucessfully against Payara, WildFly, JBoss EAP, TomEE and WebLogic. We will test the module with Thorntail (formerly WildFly Swarm) and WebSphere Liberty/Open Liberty. A contribution testing against WebSphere classic is welcome.

## Usage
To use the module, simply add the following dependency from Maven Central:

      <dependency>
        <groupId>org.axonframework</groupId>
        <artifactId>axon-cdi</artifactId>
        <version>3.3-alpha1</version>
      </dependency>

## Using Snapshot
To use the snapshot version on the master branch, you will need to build it locally for the time being. Once you have built the artifact locally, simply add the following dependency:

      <dependency>
        <groupId>org.axonframework</groupId>
        <artifactId>axon-cdi</artifactId>
        <version>3.3-alpha2-SNAPSHOT</version>
      </dependency>

### Automatic Configuration
The base Axon Framework is extremely powerful and flexible. What this extension does is to provide a number of sensible defaults for Axon applications while still allowing you reasonable configuration flexibility - including the ability to override defaults. As soon as you include the module in your project, you will be able to inject a number of Axon APIs into your code using CDI. These APIs represent the most important Axon Framework building blocks:

* [CommandBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/CommandBus.html)
* [CommandGateway](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/gateway/CommandGateway.html)
* [EventBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/EventBus.html)
* [QueryBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/queryhandling/QueryBus.html)
* [QueryGateway](http://www.axonframework.org/apidocs/3.3/org/axonframework/queryhandling/QueryGateway.html)
* [Serializer](http://www.axonframework.org/apidocs/3.3/org/axonframework/serialization/Serializer.html)
* [Configuration](http://www.axonframework.org/apidocs/3.3/org/axonframework/config/Configuration.html)
 
### Overrides
You can provide configuration overrides for the following Axon artifacts by creating CDI producers for them:
* [EntityManagerProvider](http://www.axonframework.org/apidocs/3.3/org/axonframework/common/jpa/EntityManagerProvider.html)
* [EventStorageEngine](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventsourcing/eventstore/EventStorageEngine.html)
* [TransactionManager](http://www.axonframework.org/apidocs/3.3/org/axonframework/common/transaction/TransactionManager.html) (in case of JTA, make sure this is a transaction manager that will work with JTA. For your convenience, we have provided a JtaTransactionManager that should work in most CMT and BMT situations.)
* [EventBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/EventBus.html)
* [CommandBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/CommandBus.html)
* [QueryBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/queryhandling/QueryBus.html)
* [CommandGateway](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/gateway/CommandGateway.html)
* [QueryGateway](http://www.axonframework.org/apidocs/3.3/org/axonframework/queryhandling/QueryGateway.html)
* [TokenStore](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/tokenstore/TokenStore.html)
* [Serializer](http://www.axonframework.org/apidocs/3.3/org/axonframework/serialization/Serializer.html) (both a global serializer and an event serializer may be overriden. To override an event serializer, please name the producer "eventSerializer" via the @Named annotation. It is purely optional, but you can use @Named to name your global serializer "serializer". If no @Named annotation is present, the serializer is assumed to be global)
* [ErrorHandler](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/ErrorHandler.html)
* [ListenerInvocationErrorHandler](https://github.com/AxonFramework/AxonFramework/blob/master/core/src/main/java/org/axonframework/eventhandling/ListenerInvocationErrorHandler.java)
* [CorrelationDataProvider](https://github.com/AxonFramework/AxonFramework/blob/master/core/src/main/java/org/axonframework/messaging/correlation/CorrelationDataProvider.java)
* [ModuleConfiguration](http://www.axonframework.org/apidocs/3.3/org/axonframework/config/ModuleConfiguration.html)
* [EventUpcaster](http://www.axonframework.org/apidocs/3.3/org/axonframework/serialization/upcasting/event/EventUpcaster.html)
* [Configurer](http://www.axonframework.org/apidocs/3.3/org/axonframework/config/Configurer.html)

For more details on these objects and the Axon Framework, please consult the [Axon Framework Reference Guide](https://docs.axonframework.org).
  
### Aggregates
You can define aggregate roots by placing a simple annotation `org.axonframework.cdi.stereotype.Aggregate` on your class. It will be automatically collected by the CDI container and registered.

### Event Handlers and Query Handlers
Event handlers and query handlers must be CDI beans. They will be automatically registered with Axon for you.

## Examples
Please have a look at the examples in the [example](/example) folder.

### Java EE
The [Java EE](/example/javaee) example demonstrates usage inside any Java EE 7+ compatible application server much as Payara or WildFly. The example is a generic Maven web application you should be able to build on any IDE, generate a Java EE 7 war and deploy to your favorite application server. We have so far tested sucessfully against Payara, WildFly, JBoss EAP, TomEE and WebLogic.

For convenience, we have added Cargo configurations in separate Maven profiles for most supported and tested application servers.

* To run the example against WildFly, simply execute the Maven target: `mvn package cargo:run -Pwildfly`
* To run the example against JBoss EAP, simply execute the Maven target: `mvn package cargo:run -Pjboss`
* To run the example against TomEE, simply execute the Maven target: `mvn package cargo:run -Ptomee`

### Java SE
The [Java SE](/example/javase) example demonstrates usage in a non-Java EE, Java SE environment that is CDI enabled (that could include a Servlet-only 
environment with CDI). Note that while the extension targets CDI 1.1, the example uses the CDI 2 Java SE bootstrap API. It is also easily possible to 
achieve the same functionality in CDI 1.1 using CDI implementation (such as Weld) specific Java SE bootstrap APIs (in the same vein it is possible to 
use the CDI 2 Java SE bootstrap API in Servlet containers). The example is a generic Maven Java SE application you should be able to build on any 
IDE, generate a Java SE executable jar and run from the command line using `java -jar`.

For convenience, we have added a Maven exec Java plugin in the Maven configuration. You can run the application by simply executing the 
Maven target: `mvn package exec:java`

## Advanced Usage

### Usage of JPA Event Store

If you want to use the JPA based event store inside of a container (e.g. Payara or WildFly), you have to configure the following facilities:
* [EntityManagerProvider](http://www.axonframework.org/apidocs/3.3/org/axonframework/common/jpa/EntityManagerProvider.html)
* [TransactionManager](http://www.axonframework.org/apidocs/3.3/org/axonframework/common/transaction/TransactionManager.html) (in case of JTA, make sure this is a transaction manager that will work with JTA. For your convenience, we have provided a JtaTransactionManager that should work in most CMT and BMT situations.)
* [EventStorageEngine](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventsourcing/eventstore/EventStorageEngine.html)
* [TokenStore](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/tokenstore/TokenStore.html)

Please see the examples for details.

## Roadmap
The module is currently in very early alpha state but is likely fairly feature complete. We are actively maturing it to a first release stage. We welcome early adopters and contributors.

## Known Issues
The following are the known issues with the extension. Please also look at our [GitHub issue tracker](https://github.com/AxonFramework/cdi/issues).
* We have tested but do not currently support GlassFish due to numerous critical bugs that have been fixed in GlassFish derivative Payara.

## Acknowledgements
The work for Axon to support CDI started in the community. The earliest CDI support for Axon came from the following folks:

* _[Alessio D'Innocenti](https://github.com/kamaladafrica)_
* _[Damien Clement d'Huart](https://github.com/dcdh)_

A few other folks took this community-driven work a step much further by more closely aligning with newer versions of Axon:

* _[Simon Zambrovski](https://github.com/zambrovski)/[Holisticon](https://github.com/holisticon)_
* _[Jan Galinski](https://github.com/galinski)/[Holisticon](https://github.com/holisticon)_

The official Axon support for CDI is based on the collective hard work of these folks in the community. AxonIQ is very grateful to all direct and indirect community contributors to this module.
