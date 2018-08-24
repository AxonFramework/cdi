package org.axonframework.cdi.transaction;

import java.lang.invoke.MethodHandles;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtaTransactionManager implements TransactionManager {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_LOCATION
            = "java:comp/TransactionSynchronizationRegistry";

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    @Override
    public Transaction startTransaction() {
        logger.debug("Beginning JTA transaction if required and possible.");

        UserTransaction userTransaction = getUserTransaction();

        if (userTransaction != null) {
            logger.debug("In a BMT compatible context, using UserTransaction.");

            try {
                if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
                    logger.debug("Beginning BMT transaction.");
                    userTransaction.begin();
                } else {
                    logger.warn("Cannot begin BMT transaction, current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                }
            } catch (SystemException | NotSupportedException ex) {
                logger.warn("Had trouble trying to start BMT transaction.", ex);
            }
        } else {
            TransactionSynchronizationRegistry registry = getTransactionSynchronizationRegistry();

            if (registry != null) {
                logger.debug("Most likely in a CMT compatible context, using TransactionSynchronizationRegistry.");
                logger.debug("Not allowed to begin CMT transaction, the current transaction status is {}.",
                        statusToString(registry.getTransactionStatus()));
            } else {
                logger.warn("No JTA APIs available in this context.");
            }
        }

        return new Transaction() {
            @Override
            public void commit() {
                attemptCommit();
            }

            @Override
            public void rollback() {
                attemptRollback();
            }
        };
    }

    protected void attemptCommit() {
        logger.debug("Committing JTA transaction if required and possible.");

        UserTransaction userTransaction = getUserTransaction();

        if (userTransaction != null) {
            logger.debug("In a BMT compatible context, using UserTransaction.");

            try {
                if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                    logger.debug("Committing BMT transaction.");
                    userTransaction.commit();
                } else {
                    logger.warn("Cannot commit BMT transaction, current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                }
            } catch (SystemException | RollbackException
                    | HeuristicMixedException | HeuristicRollbackException
                    | SecurityException | IllegalStateException ex) {
                logger.warn("Had trouble trying to commit BMT transaction.", ex);
            }
        } else {
            TransactionSynchronizationRegistry registry = getTransactionSynchronizationRegistry();

            if (registry != null) {
                logger.debug("Most likely in a CMT compatible context, using TransactionSynchronizationRegistry.");
                logger.debug("Not allowed to commit CMT transaction, the current transaction status is {}.",
                        statusToString(registry.getTransactionStatus()));
            } else {
                logger.warn("No JTA APIs available in this context.");
            }
        }
    }

    protected void attemptRollback() {
        logger.debug("Rolling back JTA transaction if required and possible.");

        UserTransaction userTransaction = getUserTransaction();

        if (userTransaction != null) {
            logger.debug("In a BMT compatible context, using UserTransaction.");

            try {
                if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                    logger.debug("Rolling back BMT transaction.");
                    userTransaction.rollback();
                } else {
                    logger.warn("Cannot roll back BMT transaction, current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                }
            } catch (SystemException | SecurityException | IllegalStateException ex) {
                logger.warn("Had trouble trying to roll back BMT transaction.", ex);
            }
        } else {
            TransactionSynchronizationRegistry registry = getTransactionSynchronizationRegistry();

            if (registry != null) {
                logger.debug("Most likely in a CMT compatible context, using TransactionSynchronizationRegistry.");

                if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
                    logger.debug("Setting CMT transaction to roll back.");
                    registry.setRollbackOnly();
                } else {
                    logger.warn("Cannot roll back CMT transaction, current transaction status is {}.",
                            statusToString(registry.getTransactionStatus()));
                }
            } else {
                logger.warn("No JTA APIs available in this context.");
            }
        }
    }

    protected UserTransaction getUserTransaction() {
        try {
            return (UserTransaction) new InitialContext().lookup(USER_TRANSACTION_LOCATION);
        } catch (NamingException ex) {
            logger.debug("Could not look up UserTransaction.", ex);
        }

        return null;
    }

    protected TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        try {
            return (TransactionSynchronizationRegistry) new InitialContext().lookup(
                    TRANSACTION_SYNCHRONIZATION_REGISTRY_LOCATION);
        } catch (NamingException ex) {
            logger.debug("Could not look up TransactionSynchronizationRegistry.", ex);
        }

        return null;
    }

    private String statusToString(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "Active";
            case Status.STATUS_COMMITTED:
                return "Committed";
            case Status.STATUS_COMMITTING:
                return "Commiting";
            case Status.STATUS_MARKED_ROLLBACK:
                return "Marked for rollback";
            case Status.STATUS_NO_TRANSACTION:
                return "No transaction";
            case Status.STATUS_PREPARED:
                return "Prepared";
            case Status.STATUS_PREPARING:
                return "Preparing";
            case Status.STATUS_ROLLEDBACK:
                return "Rolled back";
            case Status.STATUS_ROLLING_BACK:
                return "Rolling back";
            case Status.STATUS_UNKNOWN:
                return "Unknown";
            default:
                return null;
        }
    }
}
