package org.axonframework.cdi.example.wildfly.command;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;
import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;
import org.axonframework.eventsourcing.EventSourcingHandler;

@Aggregate
public class Account {

    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @AggregateIdentifier
    private String accountId;
    private Double overdraftLimit;

    public Account() {
        // Empty constructor needed for CDI proxying.
    }

    @CommandHandler
    public Account(final CreateAccountCommand command) {
        logger.info("Create account command handled.");

        apply(new AccountCreatedEvent(command.getAccountId(),
                command.getOverdraftLimit()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        logger.info("Applying account created event.");

        this.accountId = event.getAccountId();
        this.overdraftLimit = event.getOverdraftLimit();
    }
}
