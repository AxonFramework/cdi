package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class MoneyDepositedEvent {

    private final String accountId;
    private final BigDecimal amount;

    public MoneyDepositedEvent(String accountId, BigDecimal amount) {
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
        return "MoneyDepositedEvent [accountId=" + accountId + ", amount=" + amount + "]";
    }
}
