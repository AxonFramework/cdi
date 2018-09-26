package org.axonframework.cdi.example.javaee;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.axonframework.cdi.example.javaee.command.CreateAccountCommand;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.interceptors.EventLoggingInterceptor;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;

@Singleton
@Startup
public class AccountApplication {

    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @SuppressWarnings("cdi-ambiguous-dependency")
    @Inject
    private EventBus eventBus;

    @SuppressWarnings("cdi-ambiguous-dependency")
    @Inject
    private CommandBus commandBus;

    @SuppressWarnings("cdi-ambiguous-dependency")
    @Inject
    private CommandGateway commandGateway;

    @PostConstruct
    public void run() {
        logger.info("Initializing Account application.");

        commandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>());
        eventBus.registerDispatchInterceptor(new EventLoggingInterceptor());

        commandGateway.sendAndWait(new CreateAccountCommand(UUID.randomUUID().toString(), BigDecimal.valueOf(100.00)));
    }
}
