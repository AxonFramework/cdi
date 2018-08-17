package org.axonframework.cdi.transaction;

import java.lang.invoke.MethodHandles;
import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction manager not actually managing resources, but writing logs on
 * transaction operations.
 * <p>
 * Useful only for defaulting, placeholder and debugging purposes.
 *
 * @author Simon Zambrovski, Holisticon AG
 */
// TODO Should this be made package private to limit API visibility?
public enum LoggingTransactionManager implements TransactionManager {

    /**
     * Singleton instance of the TransactionManager.
     */
    INSTANCE;

    private final static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /**
     * Logging empty transactions.
     */
    public static final Transaction EMPTY = new Transaction() {
        @Override
        public void commit() {
            logger.warn("Logging transaction committed. Use for debugging only.");
        }

        @Override
        public void rollback() {
            logger.warn("Logging transaction rolled back. Use for debugging only.");
        }
    };

    @Override
    public Transaction startTransaction() {
        logger.warn("Logging transaction started. Use for debugging only.");
        return EMPTY;
    }
}
