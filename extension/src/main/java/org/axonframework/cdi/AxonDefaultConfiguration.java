package org.axonframework.cdi;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE)
public class AxonDefaultConfiguration implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());
    
    @Produces
    @ApplicationScoped
    public CommandGateway commandGateway(Configuration configuration) {
        return configuration.commandGateway();
    }

    void init(@Observes @Initialized(ApplicationScoped.class) Object initialized) {
        logger.info("Ensuring Axon configuration is started.");
    }
}
