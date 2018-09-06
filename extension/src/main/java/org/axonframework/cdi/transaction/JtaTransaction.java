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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtaTransaction implements Transaction {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String JBOSS_USER_TRANSACTION_LOCATION
            = "java:jboss/UserTransaction";
    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_LOCATION
            = "java:comp/TransactionSynchronizationRegistry";

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private UserTransaction userTransaction = null;
    private TransactionSynchronizationRegistry registry;
    private boolean owned = true;

    public JtaTransaction() {
        detectContext();
        attemptBegin();
    }

    @Override
    public void commit() {
        attemptCommit();
    }

    @Override
    public void rollback() {
        attemptRollback();
    }

    private void detectContext() {
        userTransaction = getUserTransaction();

        if (userTransaction != null) {
            logger.debug("In a BMT compatible context, using UserTransaction.");

            try {
                if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                    logger.debug("We cannot own the BMT transaction, the current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                    owned = false;
                }
            } catch (SystemException ex) {
                logger.warn("Had trouble trying to get BMT transaction status.", ex);
                owned = false;
            }
        } else {
            registry = getTransactionSynchronizationRegistry();

            if (registry != null) {
                logger.debug("Most likely in a CMT compatible context, using TransactionSynchronizationRegistry.");
            } else {
                logger.warn("No JTA APIs available in this context. No transation managment can be performed.");
            }
        }
    }

    private void attemptBegin() {
        logger.debug("Beginning JTA transaction if required and possible.");

        if (userTransaction != null) {
            try {
                if (owned) {
                    logger.debug("Beginning BMT transaction.");
                    userTransaction.begin();
                } else {
                    logger.debug("Did not try to begin non-owned BMT transaction.");
                }
            } catch (SystemException | NotSupportedException ex) {
                logger.warn("Had trouble trying to start BMT transaction.", ex);
            }
        } else {
            if (registry != null) {
                logger.debug("Not allowed to begin CMT transaction, the current transaction status is {}.",
                        statusToString(registry.getTransactionStatus()));
            } else {
                logger.warn("No JTA APIs available in this context. No begin done.");
            }
        }
    }

    private void attemptCommit() {
        logger.debug("Committing JTA transaction if required and possible.");

        if (userTransaction != null) {
            try {
                if (owned) {
                    if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        logger.debug("Committing BMT transaction.");
                        userTransaction.commit();
                    } else {
                        logger.warn("Cannot commit BMT transaction, current transaction status is {}.",
                                statusToString(userTransaction.getStatus()));
                    }
                } else {
                    logger.debug("Cannot commit non-owned BMT transaction.");
                }
            } catch (SystemException | RollbackException
                    | HeuristicMixedException | HeuristicRollbackException
                    | SecurityException | IllegalStateException ex) {
                logger.warn("Had trouble trying to commit BMT transaction.", ex);
            }
        } else {
            if (registry != null) {
                logger.debug("Not allowed to commit CMT transaction, the current transaction status is {}.",
                        statusToString(registry.getTransactionStatus()));
            } else {
                logger.warn("No JTA APIs available in this context. No commit done.");
            }
        }
    }

    private void attemptRollback() {
        logger.debug("Rolling back JTA transaction if required and possible.");

        if (userTransaction != null) {
            try {
                if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                    if (owned) {
                        logger.debug("Rolling back BMT transaction.");
                        userTransaction.rollback();
                    } else {
                        logger.debug("Setting rollback for non-owned BMT transaction.");
                        userTransaction.setRollbackOnly();
                    }
                } else {
                    logger.warn("Cannot roll back BMT transaction, current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                }
            } catch (SystemException | SecurityException | IllegalStateException ex) {
                logger.warn("Had trouble trying to roll back BMT transaction.", ex);
            }
        } else {
            if (registry != null) {
                if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
                    logger.debug("Setting CMT transaction to roll back.");
                    registry.setRollbackOnly();
                } else {
                    logger.warn("Cannot roll back CMT transaction, current transaction status is {}.",
                            statusToString(registry.getTransactionStatus()));
                }
            } else {
                logger.warn("No JTA APIs available in this context. No rollback performed.");
            }
        }
    }

    private UserTransaction getUserTransaction() {
        try {
            logger.debug("Attempting to look up standard UserTransaction.");
            return (UserTransaction) new InitialContext().lookup(
                    USER_TRANSACTION_LOCATION);
        } catch (NamingException ex) {
            logger.debug("Could not look up standard UserTransaction.", ex);

            try {
                logger.debug("Attempting to look up JBoss proprietary UserTransaction.");
                return (UserTransaction) new InitialContext().lookup(
                        JBOSS_USER_TRANSACTION_LOCATION);
            } catch (NamingException ex1) {
                logger.debug("Could not look up JBoss proprietary UserTransaction.", ex1);
            }
        }

        return null;
    }

    private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
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
