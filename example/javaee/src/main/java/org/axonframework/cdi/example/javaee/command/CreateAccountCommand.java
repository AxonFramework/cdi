package org.axonframework.cdi.example.javaee.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

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

    @Override
    public String toString() {
        return "Create Account Command with accountId=" + accountId
                + ", overdraftLimit=" + overdraftLimit;
    }
}
