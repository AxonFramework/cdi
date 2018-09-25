package org.axonframework.cdi.example.javase.query;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.axonframework.cdi.example.javase.command.AccountCreatedEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;

@ApplicationScoped
public class AccountProjection {

    private Map<String, Double> accounts = new HashMap<>();

    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @Inject
    private CommandGateway commandGateway;

    @EventHandler
    public void on(AccountCreatedEvent event) {
        logger.log(Level.INFO, "Projecting: {0}.", event);
        accounts.put(event.getAccountId(), event.getOverdraftLimit());
    }

    @QueryHandler
    public Double query(String accountId) {
        logger.log(Level.INFO, "Querying: {0}.", accountId);
        return accounts.get(accountId);
    }
}
