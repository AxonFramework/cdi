package org.axonframework.cdi.transaction;

import org.axonframework.common.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.*;
import java.lang.invoke.MethodHandles;

/**
 *
 */
public class JtaTransaction implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final UserTransaction userTransaction;
    private final TransactionSynchronizationRegistry registry;
    private boolean owned = true;

    JtaTransaction(final UserTransaction userTransaction, final TransactionSynchronizationRegistry registry) {
        this.userTransaction = userTransaction;
        this.registry = registry;

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

    /**
     * Detect how we're going to do transactions and what the current transaction's status is.
     */
    private void detectContext() {
        if (userTransaction != null) {
            logger.debug("In a BMT compatible context, using UserTransaction.");

            try {
                if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                    logger.warn("We cannot own the BMT transaction, the current transaction status is {}.",
                            statusToString(userTransaction.getStatus()));
                    owned = false;
                }
            } catch (SystemException ex) {
                logger.warn("Had trouble trying to get BMT transaction status.", ex);
                owned = false;
            }
        } else if (registry != null) {
            logger.debug("Most likely in a CMT compatible context, using TransactionSynchronizationRegistry.");
        } else {
            logger.warn("No JTA APIs available in this context. No transation managment can be performed.");
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
                    logger.warn("Did not try to begin non-owned BMT transaction.");
                }
            } catch (SystemException | NotSupportedException ex) {
                logger.warn("Had trouble trying to start BMT transaction.", ex);
            }
        } else if (registry != null) {
            logger.error("Not allowed to begin CMT transaction, the current transaction status is {}.",
                    statusToString(registry.getTransactionStatus()));
        } else {
            logger.warn("No JTA APIs available in this context. No begin done.");
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
                    logger.error("Cannot commit non-owned BMT transaction.");
                }
            } catch (SystemException | RollbackException
                    | HeuristicMixedException | HeuristicRollbackException
                    | SecurityException | IllegalStateException ex) {
                logger.warn("Had trouble trying to commit BMT transaction.", ex);
            }
        } else if (registry != null) {
            logger.error("Not allowed to commit CMT transaction, the current transaction status is {}.",
                    statusToString(registry.getTransactionStatus()));
        } else {
            logger.warn("No JTA APIs available in this context. No commit done.");
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
        } else if (registry != null) {
            if (registry.getTransactionStatus() == Status.STATUS_ACTIVE) {
                logger.warn("Setting CMT transaction to roll back.");
                registry.setRollbackOnly();
            } else {
                logger.warn("Cannot roll back CMT transaction, current transaction status is {}.",
                        statusToString(registry.getTransactionStatus()));
            }
        } else {
            logger.warn("No JTA APIs available in this context. No rollback performed.");
        }
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
