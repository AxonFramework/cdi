package org.axonframework.cdi.example.javaee;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.axonframework.cdi.example.javaee.command.CreateAccountCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.interceptors.EventLoggingInterceptor;

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
        logger.info("Initializing Account application.");

        eventBus.registerDispatchInterceptor(new EventLoggingInterceptor());

        commandGateway.send(new CreateAccountCommand(
                UUID.randomUUID().toString(), 100.00));
    }
}
