package org.axonframework.cdi.example.javaee.command;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
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
    @SuppressWarnings("unused")
	private Double overdraftLimit;

    public Account() {
        // Empty constructor needed for CDI proxying.
    }

    @CommandHandler
    public Account(final CreateAccountCommand command) {
        logger.log(Level.INFO, "Handling: {0}.", command);

        apply(new AccountCreatedEvent(command.getAccountId(),
                command.getOverdraftLimit()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        logger.log(Level.INFO, "Applying: {0}.", event);

        this.accountId = event.getAccountId();
        this.overdraftLimit = event.getOverdraftLimit();
    }
}
