package org.axonframework.cdi.transaction;

import java.lang.invoke.MethodHandles;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container transaction manager.
 * <p>
 * Uses provided entity manager and accesses JTA UserTransaction from JNDI
 * provided by the container.
 * </p>
 *
 * @author Simon Zambrovski
 */
// TODO Check if this is really needed as part of the API or if the user simply
// supplies a JNDI entry and the entity manager. How is it done in the Spring
// support? A minor concern.
public class ContainerTransactionManager implements TransactionManager {

    private final static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private final String userTransactionJndiName;

    /**
     * Constructs the transaction manager.
     *
     * @param entityManager entity manager to use.
     * @param userTransactionJndiName JNDI address of the USerTransaction object
     * provided by the container.
     */
    public ContainerTransactionManager(final EntityManager entityManager,
            final String userTransactionJndiName) {
        this.entityManager = entityManager;
        this.userTransactionJndiName = userTransactionJndiName;
    }

    @Override
    public Transaction startTransaction() {

        // Start with empty logging transaction.
        Transaction startedTransaction = LoggingTransactionManager.EMPTY;

        try {
            final UserTransaction transaction
                    = (UserTransaction) new InitialContext().lookup(
                            this.userTransactionJndiName);

            if (transaction == null) {
                logger.warn("No transaction is available.");

                return startedTransaction;
            }

            if (transaction.getStatus() != Status.STATUS_ACTIVE) {
                logger.trace("Creating a new transaction.");

                transaction.begin();
            } else {
                logger.trace("Re-using running transaction with status {}.",
                        transaction.getStatus());
            }

            // Join transaction.
            if (!this.entityManager.isJoinedToTransaction()) {
                this.entityManager.joinTransaction();
            }

            startedTransaction = new Transaction() {
                @Override
                public void commit() {
                    try {
                        switch (transaction.getStatus()) {
                            case Status.STATUS_ACTIVE:
                                logger.trace("Committing transaction.");
                                transaction.commit();
                                break;
                            case Status.STATUS_MARKED_ROLLBACK:
                                logger.warn("Transaction has been marked as rollback-only.");
                                rollback();
                                break;
                            default:
                                logger.warn("Ignored commit of non-active transaction in status {}.",
                                        transaction.getStatus());
                                break;
                        }
                    } catch (final IllegalStateException | SystemException
                            | SecurityException | RollbackException
                            | HeuristicMixedException
                            | HeuristicRollbackException e) {
                        logger.error("Error committing transaction.", e);
                    }
                }

                @Override
                public void rollback() {
                    try {
                        switch (transaction.getStatus()) {
                            case Status.STATUS_ACTIVE:
                            // Intended no break.
                            case Status.STATUS_MARKED_ROLLBACK:
                                logger.trace("Rolling transaction back.");
                                transaction.rollback();
                                break;
                            default:
                                logger.warn("Ignored rollback of non-active transaction in status {}.",
                                        transaction.getStatus());
                                break;
                        }
                    } catch (final IllegalStateException | SystemException
                            | SecurityException e) {
                        logger.error("Error rolling transaction back.", e);
                    }
                }
            };
        } catch (final NotSupportedException | SystemException | NamingException e) {
            logger.error("Error retrieving user transaction.", e);
        }

        return startedTransaction;
    }
}