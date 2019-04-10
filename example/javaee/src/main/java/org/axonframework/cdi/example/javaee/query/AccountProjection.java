package org.axonframework.cdi.example.javaee.query;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.axonframework.cdi.example.javaee.command.AccountCreatedEvent;
import org.axonframework.cdi.example.javaee.command.MoneyDepositedEvent;
import org.axonframework.cdi.example.javaee.command.MoneyWithdrawnEvent;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;

@ApplicationScoped
public class AccountProjection {

    private static final Logger logger = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    @Inject
    private EntityManager entityManager;

    @EventHandler
    public void on(AccountCreatedEvent event) {
        logger.log(Level.INFO, "Projecting: {0}.", event);

        AccountSummary accountSummary = new AccountSummary();
        accountSummary.setAccountId(event.getAccountId());
        accountSummary.setCurrentBalance(BigDecimal.ZERO);
        accountSummary.setMaxWithdrawAmount(event.getOverdraftLimit());
        entityManager.persist(accountSummary);
    }

    @EventHandler
    public void on(MoneyWithdrawnEvent event) {
        logger.log(Level.INFO, "Projecting: {0}.", event);
        AccountSummary accountSummary = entityManager.find(AccountSummary.class, event.getAccountId());
        accountSummary.setCurrentBalance(accountSummary.getCurrentBalance().subtract(event.getAmount()));
        accountSummary.setMaxWithdrawAmount(accountSummary.getMaxWithdrawAmount().subtract(event.getAmount()));
    }

    @EventHandler
    public void on(MoneyDepositedEvent event) {
        logger.log(Level.INFO, "Projecting: {0}.", event);
        AccountSummary accountSummary = entityManager.find(AccountSummary.class, event.getAccountId());
        accountSummary.setCurrentBalance(accountSummary.getCurrentBalance().add(event.getAmount()));
        accountSummary.setMaxWithdrawAmount(accountSummary.getMaxWithdrawAmount().add(event.getAmount()));
    }

    @QueryHandler
    public List<AccountSummary> getAccountSummaries(AccountSummaryQuery query) {
        return entityManager.createQuery("SELECT s FROM AccountSummary s", AccountSummary.class).getResultList();
    }
}
