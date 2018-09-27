package org.axonframework.cdi.example.javaee.query;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.axonframework.cdi.example.javaee.command.AccountCreatedEvent;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;

@ApplicationScoped
public class AccountProjection {
    
    @Inject
    private EventBus eventBus;
            
    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @EventHandler
    public void on(AccountCreatedEvent event) {
        logger.log(Level.INFO, "Event Bus: {0}.", eventBus);
        logger.log(Level.INFO, "Projecting: {0}.", event);
    }
}
