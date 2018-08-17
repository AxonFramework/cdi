package org.axonframework.cdi.example.wildfly.command;

public class AccountCreatedEvent {

    private final String accountId;
    private final Double overdraftLimit;

    public AccountCreatedEvent(String accountId, Double overdraftLimit) {
        this.accountId = accountId;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public Double getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public String toString() {
        return "Account Created Event with accountId=" + accountId
                + ", overdraftLimit=" + overdraftLimit;
    }
}
