package org.axonframework.cdi.example.javaee.command;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
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
    private BigDecimal overdraftLimit;

    private BigDecimal balance;

    public Account() {
        // Empty constructor needed for CDI proxying.
    }

    @CommandHandler
    public Account(final CreateAccountCommand command) {
        logger.log(Level.INFO, "Handling: {0}.", command);

        apply(new AccountCreatedEvent(command.getAccountId(), command.getOverdraftLimit()));
    }

    @CommandHandler
    public void withdraw(final WithdrawMoneyCommand command) throws WithdrawalDeniedException {
        BigDecimal maxWithdraw = balance.add(overdraftLimit);
        if (maxWithdraw.compareTo(command.getAmount()) < 0)
            throw new WithdrawalDeniedException(command.getAccountId(), command.getAmount());

        apply(new MoneyWithdrawnEvent(command.getAccountId(), command.getAmount()));
    }

    @CommandHandler
    public void deposit(final DepositMoneyCommand command) {
        apply(new MoneyDepositedEvent(command.getAccountId(), command.getAmount()));
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        logger.log(Level.INFO, "Applying: {0}.", event);

        this.accountId = event.getAccountId();
        this.balance = BigDecimal.ZERO;
        this.overdraftLimit = event.getOverdraftLimit();
    }

    @EventSourcingHandler
    public void on(MoneyDepositedEvent event) {
        logger.log(Level.INFO, "Applying: {0}.", event);

        this.balance = this.balance.add(event.getAmount());
    }

    @EventSourcingHandler
    public void on(MoneyWithdrawnEvent event) {
        logger.log(Level.INFO, "Applying: {0}.", event);

        this.balance = this.balance.subtract(event.getAmount());
    }
}
