package org.axonframework.cdi.example.javase;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;
import org.axonframework.cdi.example.javase.command.CreateAccountCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.interceptors.EventLoggingInterceptor;

@ApplicationScoped
public class AccountApplication {

    @Inject
    private EventBus eventBus;

    @Inject
    private CommandGateway commandGateway;

    public void run() {
        eventBus.registerDispatchInterceptor(new EventLoggingInterceptor());
        commandGateway.send(new CreateAccountCommand("4711", 1000D));
    }

    public static void main(final String[] args) {
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();

        try (SeContainer container = initializer.initialize()) {
            container.select(AccountApplication.class).get().run();
        }
    }
}
