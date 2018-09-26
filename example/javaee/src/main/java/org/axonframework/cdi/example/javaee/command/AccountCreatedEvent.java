package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class AccountCreatedEvent {

    private final String accountId;
    private final BigDecimal overdraftLimit;

    public AccountCreatedEvent(String accountId, BigDecimal overdraftLimit) {
        this.accountId = accountId;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public String toString() {
        return "AccountCreatedEvent [accountId=" + accountId + ", overdraftLimit=" + overdraftLimit + "]";
    }
}
