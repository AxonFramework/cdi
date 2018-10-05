package org.axonframework.cdi;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Mark: I began this code in the hopes of moving most of the Axon 
// configuration here to a more declarative style and also moving the bean 
// instantiation to as late in the cycle as possible. The issue is that
// it results in a circular reference that I don't think can be resolved other
// than in a programmatic fashion in an extension. The circularity is 
// demonstarted in the commented out code. Honestly, I really wanted this
// approach to work but I don't think it will unless you can see a way.
@ApplicationScoped
@Alternative
@Priority(javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE)
public class AxonDefaultConfiguration implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

//    @Produces
//    @ApplicationScoped
//    public Configuration configuration(Configurer configurer, Serializer serializer) {
//        logger.info("Starting Axon Framework configuration.");
//
//        configurer.configureSerializer(c -> serializer);
//
//        logger.info("Starting Axon configuration.");
//
//        return configurer.start();
//    }

    @Produces
    @ApplicationScoped
    public CommandGateway commandGateway(Configuration configuration) {
        return configuration.commandGateway();
    }

//    @Produces
//    @ApplicationScoped
//    public CommandBus commandBus(Configuration configuration) {
//        return configuration.commandBus();
//    }

//    @Produces
//    @ApplicationScoped
//    public QueryGateway queryGateway(Configuration configuration) {
//        return configuration.queryGateway();
//    }

//    @Produces
//    @ApplicationScoped
//    public QueryBus queryBus(Configuration configuration) {
//        return configuration.queryBus();
//    }

//    @Produces
//    @ApplicationScoped
//    public QueryUpdateEmitter queryUpdateEmitter(Configuration configuration) {
//        return configuration.queryUpdateEmitter();
//    }

//    @Produces
//    @ApplicationScoped
//    public EventBus eventBus(Configuration configuration) {
//        return configuration.eventBus();
//    }
//
//    @Produces
//    @ApplicationScoped
//    public Serializer serializer(Configuration configuration) {
//        return configuration.serializer();
//    }
}
