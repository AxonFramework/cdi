package org.axonframework.cdi.transaction;

import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * TransactionManager implementation that uses a
 * {@link org.springframework.transaction.JtaTransactionManager} as underlying
 * transaction manager.
 *
 * @author Allard Buijze
 */
public class ContainerTransactionManager implements TransactionManager {

    private final JtaTransactionManager transactionManager;

    public ContainerTransactionManager() {
        this.transactionManager = new JtaTransactionManager();
    }

    @Override
    public Transaction startTransaction() {
        TransactionStatus status = transactionManager.getTransaction(
                new DefaultTransactionDefinition());
        return new Transaction() {
            @Override
            public void commit() {
                commitTransaction(status);
            }

            @Override
            public void rollback() {
                rollbackTransaction(status);
            }
        };
    }

    /**
     * Commits the transaction with given {@code status} if the transaction is
     * new and not completed.
     *
     * @param status The status of the transaction to commit
     */
    protected void commitTransaction(TransactionStatus status) {
        if (status.isNewTransaction() && !status.isCompleted()) {
            transactionManager.commit(status);
        }
    }

    /**
     * Rolls back the transaction with given {@code status} if the transaction
     * is new and not completed.
     *
     * @param status The status of the transaction to roll back
     */
    protected void rollbackTransaction(TransactionStatus status) {
        if (status.isNewTransaction() && !status.isCompleted()) {
            transactionManager.rollback(status);
        }
    }
}
