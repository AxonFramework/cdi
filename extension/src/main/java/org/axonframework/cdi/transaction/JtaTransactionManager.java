package org.axonframework.cdi.transaction;

import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;

public class JtaTransactionManager implements TransactionManager {

    @Override
    public Transaction startTransaction() {
        return new JtaTransaction();
    }
}
