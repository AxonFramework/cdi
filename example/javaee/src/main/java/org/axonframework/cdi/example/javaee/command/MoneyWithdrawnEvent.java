package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class MoneyWithdrawnEvent {

    private final String accountId;
    private final BigDecimal amount;

    public MoneyWithdrawnEvent(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "MoneyWithdrawnEvent [accountId=" + accountId + ", amount=" + amount + "]";
    }
}
