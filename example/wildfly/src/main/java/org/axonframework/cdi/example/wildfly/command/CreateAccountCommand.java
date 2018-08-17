package org.axonframework.cdi.example.wildfly.command;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

public class CreateAccountCommand {

    @TargetAggregateIdentifier
    private final String accountId;
    private final Double overdraftLimit;

    public CreateAccountCommand(String accountId, Double overdraftLimit) {
        this.accountId = accountId;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public Double getOverdraftLimit() {
        return overdraftLimit;
    }
}
