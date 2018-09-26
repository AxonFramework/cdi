package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

public class WithdrawalDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String accountId;
    private final BigDecimal amount;

    public WithdrawalDeniedException(String accountId, BigDecimal amount) {
        super("Withrawal of " + amount + " from " + accountId + " was denied.");
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

}
