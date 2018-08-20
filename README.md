# Axon Framework CDI Support

This Axon Framework module provides support for the CDI programming model. It is a CDI portable extension integrating the Axon Framework and providing some intelligent defaults while still allowing for configuring overrides.

The current minimum supported versions are:

 * Axon Framework 3.1.1
 * CDI 1.1/Java EE 7
 
We have so far tested sucessfully against Payara and WildFly. We will test the module with JBoss EAP, Thorntail (formerly WildFly Swarm), WebSphere Liberty/Open Liberty and TomEE. Contributions testing against WebSphere classic and WebLogic are welcome. We have tested but do not currently support GlassFish due to numerous critical bugs that have been fixed in GlassFish derivative Payara.

## Usage

The artifact is not yet released to Maven Central and you have to build it locally for the time being. Once you have built the artifact locally, simply add the following dependency:

      <dependency>
        <groupId>org.axonframework</groupId>
        <artifactId>axon-cdi</artifactId>
        <version>0.1-SNAPSHOT</version>
      </dependency>

### Automatic Configuration

The base Axon Framework is extremely powerful and flexible. What this extension does is to provide a number of sensible defaults for Axon applications while still allowing you reasonable configutation flexibility - including the ability to override defaults. As soon as you include the module in your project, you will be able to inject a number of Axon APIs into your code using CDI. These APIs represent the most important Axon Framework building blocks:

 * [CommandBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/CommandBus.html)
 * [CommandGateway](http://www.axonframework.org/apidocs/3.3/org/axonframework/commandhandling/gateway/CommandGateway.html)
 * [EventBus](http://www.axonframework.org/apidocs/3.3/org/axonframework/eventhandling/EventBus.html)
 * [Serializer](http://www.axonframework.org/apidocs/3.3/org/axonframework/serialization/Serializer.html)
 * [Configuration](http://www.axonframework.org/apidocs/3.3/org/axonframework/config/Configuration.html)
 
 For more details on these objects and the Axon Framework, please consult the [Axon Framework Reference Guide](https://docs.axonframework.org).
  
### Aggregates

You can define aggregate roots by placing a simple annotation `org.axonframework.cdi.stereotype.Aggregate` on your class. It will be automatically collected by the CDI container and registered.

## Examples
Please have a look at the examples in the [example](/example) folder.

### Java EE
The [Java EE](/example/javaee) example demonstrates usage inside any Java EE 7+ compatible application server much as Payara or WildFly. The example is a generic Maven web application you should be able to build on any IDE, generate a Java EE 7 war and deploy to your favorite application server. We have so far tested sucessfully against Payara and WildFly.

For convenience, we have added Cargo configurations in separate Maven profiles for each supported and tested application server.

* To run the example against WildFly, simply execute the Maven target: `mvn package cargo:run -Pwildfly`

## Advanced Usage

### Usage of JPA Event Store

If you want to use the JPA based event store inside of a container (e.g. Payara or WildFly), you have to configure the following facilities:

  *  EntityManagerProvider
  *  TransactionManager
  *  EventStorageEngine
  *  TokenStore
  
Please see the examples for details.

## Roadmap
The module currently does not support sagas and snapshoters. Such support will be added as soon as possible. We will also add support for Axon 3.3 in the near term.

## Acknowledgements
The work for Axon to support CDI started in the community. The earliest CDI support for Axon came from the following folks:

* _[Alessio D'Innocenti](https://github.com/kamaladafrica)_
* _[Damien Clement d'Huart](https://github.com/dcdh)_

A few other folks took this community-driven work a step much further by more closely aligning with newer versions of Axon:

* _[Simon Zambrovski](https://github.com/zambrovski)/[Holisticon](https://github.com/holisticon)_
* _[Jan Galinski](https://github.com/galinski)/[Holisticon](https://github.com/holisticon)_
* _[Alex](https://github.com/alexmacavei)_

The official Axon support for CDI is based on the collective hard work of these folks in the community. AxonIQ is very grateful to all direct and indirect community contributors to this module.
