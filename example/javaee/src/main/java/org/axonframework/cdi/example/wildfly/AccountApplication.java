package org.axonframework.cdi.example.wildfly;

import org.axonframework.cdi.example.wildfly.command.CreateAccountCommand;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.interceptors.EventLoggingInterceptor;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class AccountApplication {

    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @Inject
    private EventBus eventBus;

    @Inject
    private CommandGateway commandGateway;

    @PostConstruct
    public void run() {
        logger.info("Initializing CDI application.");

        eventBus.registerDispatchInterceptor(new EventLoggingInterceptor());

        commandGateway.send(new CreateAccountCommand("4711", 100.00));
    }
}
